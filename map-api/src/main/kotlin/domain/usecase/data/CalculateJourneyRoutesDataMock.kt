package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStopId
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import java.time.LocalTime
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class CalculateJourneyRoutesDataMock(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
): CalculateJourneyRoutesUseCase {
    companion object {
        private val geometryFactory: GeometryFactory = GeometryFactory(PrecisionModel(), 4326)
        private val CZ_BBOX = geometryFactory.createPoint(Coordinate(12.6296776, 50.7374067)) to geometryFactory.createPoint(Coordinate(18.1876545, 49.0192903))
        private const val KILOMETER_TO_DEGREE = 0.008_983
        private const val AVG_STEP_LENGTH = KILOMETER_TO_DEGREE / 20
        private const val AVG_SPEED_DPM = 50.0 * KILOMETER_TO_DEGREE / 60.0
        private var generatedId = 0
    }

    private fun interpolate(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    private fun randomCoordinate(): Coordinate = Coordinate(
        interpolate(CZ_BBOX.first.x, CZ_BBOX.second.x, Random.nextDouble()),
        interpolate(CZ_BBOX.first.y, CZ_BBOX.second.y, Random.nextDouble())
    )

    private fun angleBetween(a: Coordinate, b: Coordinate): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return atan2(dy, dx)
    }

    private fun stopDistancesPrefixSum(stops: List<DbScheduledStop>) = stops
        .fold<DbScheduledStop, Pair<ArrayList<Double>, LocalTime>?>(null) { acc, curStop ->
            if (acc == null) {
                return@fold arrayListOf(0.0) to (curStop.departure ?: curStop.arrival!!)
            }
            val arrivalTimeSeconds = (curStop.arrival ?: curStop.departure!!).toSecondOfDay()
            val prevDepartureTimeSeconds = acc.second.toSecondOfDay()
            val secondsDiff = if (arrivalTimeSeconds > prevDepartureTimeSeconds) arrivalTimeSeconds - prevDepartureTimeSeconds
            else 24 * 60 * 60 - prevDepartureTimeSeconds + arrivalTimeSeconds
            val distance = secondsDiff * AVG_SPEED_DPM / 60.0
            acc.first.add(acc.first.last() + distance)
            return@fold acc.first to (curStop.departure ?: curStop.arrival!!)
        }!!.first

    private fun assignRoute(journey: DbJourney) {
        val centerOfMass = randomCoordinate()
        var currentCoord = randomCoordinate()
        var angle = (Random.nextDouble() * 2 - 1.0) * PI
        val javaRandom = java.util.Random()
        val path = mutableListOf(currentCoord)
        val routeMarkers = mutableListOf<Int>()

        val distancePrefixSum = stopDistancesPrefixSum(journey.schedule)
        var cumulativeDistance = 0.0
        for (idx in journey.schedule.indices) {
            while (cumulativeDistance < distancePrefixSum[idx]) {
                val stepLength = javaRandom.nextExponential() * AVG_STEP_LENGTH
                val nextCoord = Coordinate(currentCoord.x + stepLength * cos(angle), currentCoord.y + stepLength * sin(angle))
                path.add(nextCoord)
                val angleMean = interpolate(angle, angleBetween(centerOfMass, nextCoord), 0.1)
                angle = javaRandom.nextGaussian(angleMean, 0.6)
                currentCoord = nextCoord
                cumulativeDistance += stepLength
            }
            routeMarkers.add(path.lastIndex)
        }

        val routeStops = mutableListOf<DbRouteStop>()
        val route = DbRoute(
            relationalId = null,
            externalId = (++generatedId).toString(),
            pointSequence = geometryFactory.createLineString(path.toTypedArray()),
            routeStops = routeStops,
        )
        for ((marker, idx) in routeMarkers.withIndex()) {
            routeStops.add(DbRouteStop(
                stopId = DbRouteStopId(route.relationalId, idx),
                route = route,
                physicalStop = DbPhysicalStop(
                    relationalId = null,
                    externalId = UUID.randomUUID().toString(),
                    name = "random",
                    position = geometryFactory.createPoint(path[marker]),
                    tags = emptyMap(),
                ),
                pointSequenceIndex = marker,
            ))
        }
        journey.route = route
    }

    override fun calculateRoutes() {
        val pageSize = 30
        var currentPage: Page<DbJourney>
        do {
            currentPage = journeyJpaRepository.findByNullRoute(PageRequest.of(0, pageSize))
            for (journey in currentPage.content) {
                assignRoute(journey)
            }
            val newRoutes = currentPage.content.mapNotNull { it.route }
            val newRouteStops = newRoutes.flatMap { it.routeStops }
            val newPhysicalStops = newRouteStops.map { it.physicalStop }
            physicalStopJpaRepository.saveAll(newPhysicalStops)
            routeJpaRepository.saveAll(newRoutes)
            routeStopJpaRepository.saveAll(newRouteStops)
            journeyJpaRepository.saveAll(currentPage.content)
        } while (currentPage.totalPages != 1)
    }
}