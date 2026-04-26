package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.JourneyByDistinctJourneyPatternDto
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalTime
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Component
class CalculateJourneyRoutesMock(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val journeyPatternJpaRepository: JourneyPatternJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    @PersistenceContext
    private val entityManager: EntityManager,
): CalculateJourneyRoutesUseCase {
    companion object {
        private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        private val CZ_BBOX = geometryFactory.createPoint(Coordinate(12.6296776, 50.7374067)) to geometryFactory.createPoint(Coordinate(18.1876545, 49.0192903))
        private const val KILOMETER_TO_DEGREE = 0.008_983
        private const val STEP_LENGTH = KILOMETER_TO_DEGREE / 8
        private const val AVG_SPEED_DPM = 50.0 * KILOMETER_TO_DEGREE / 60.0
        private const val PAGE_SIZE = 30
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

    private fun stopDistancesPrefixSum(schedule: List<ScheduledStopDto>) = schedule
        .fold<ScheduledStopDto, Pair<ArrayList<Double>, LocalTime>?>(null) { acc, curStop ->
            if (acc == null) {
                return@fold arrayListOf(0.0) to (curStop.departure ?: curStop.arrival!!)
            }
            val arrivalTimeSeconds = (curStop.arrival ?: curStop.departure!!).toSecondOfDay()
            val prevDepartureTimeSeconds = acc.second.toSecondOfDay()
            val secondsDiff = if (arrivalTimeSeconds >= prevDepartureTimeSeconds) arrivalTimeSeconds - prevDepartureTimeSeconds
            else 24 * 60 * 60 - prevDepartureTimeSeconds + arrivalTimeSeconds
            val distance = secondsDiff * AVG_SPEED_DPM / 60.0
            acc.first.add(acc.first.last() + distance)
            return@fold acc.first to (curStop.departure ?: curStop.arrival!!)
        }!!.first

    private fun createRoute(schedule: List<ScheduledStopDto>): Route {
        val centerOfMass = randomCoordinate()
        var currentCoord = randomCoordinate()
        var angle = (Random.nextDouble() * 2 - 1.0) * PI
        val javaRandom = java.util.Random()
        val path = mutableListOf(currentCoord)
        val routeMarkers = mutableListOf(0)
        val stopDistances = mutableListOf(0.0)

        val distancePrefixSum = stopDistancesPrefixSum(schedule)
        var cumulativeDistance = 0.0
        for (idx in 1 until schedule.size) {
             do {
                val nextCoord = Coordinate(currentCoord.x + STEP_LENGTH * cos(angle), currentCoord.y + STEP_LENGTH * sin(angle))
                path.add(nextCoord)
                val angleMean = interpolate(angle, angleBetween(centerOfMass, nextCoord), 0.1)
                angle = javaRandom.nextGaussian(angleMean, 0.6)
                currentCoord = nextCoord
                cumulativeDistance += STEP_LENGTH
            } while (cumulativeDistance < distancePrefixSum[idx])
            routeMarkers.add(path.lastIndex)
            stopDistances.add(cumulativeDistance)
        }

        val routeStops = mutableListOf<RouteStop>()
        val route = Route(
            relationalId = null,
            externalId = UUID.randomUUID().toString(),
            pointSequence = geometryFactory.createLineString(path.toTypedArray()),
            routeStops = routeStops,
            totalDistance = cumulativeDistance,
        )
        for ((idx, marker) in routeMarkers.withIndex()) {
            routeStops.add(RouteStop(
                stopId = RouteStopId(route.relationalId, idx),
                route = route,
                physicalStop = PhysicalStop(
                    relationalId = null,
                    externalId = UUID.randomUUID().toString(),
                    name = "random",
                    position = geometryFactory.createPoint(path[marker]),
                    tags = emptyMap(),
                ),
                routeFraction = stopDistances[idx] / cumulativeDistance,
            ))
        }
        return route
    }

    override fun calculateRoutes() {
        var currentPage: Page<JourneyByDistinctJourneyPatternDto>? = null
        do { transactionTemplate.executeWithoutResult {
            currentPage = journeyJpaRepository
                .findAllDistinctJourneyPatternDtoWithNullRoute(PageRequest.of(0, PAGE_SIZE))
            val scheduledStops1 = scheduledStopJpaRepository
                .findAllDtoByJourneyIds(currentPage.content.map(JourneyByDistinctJourneyPatternDto::relationalId))
            val scheduledStops = scheduledStops1
                .groupBy(ScheduledStopDto::journeyId)
                .mapValues { (_, stops) -> stops.sortedBy(ScheduledStopDto::stopOrder) }
            val newRoutesMap = scheduledStops.mapValues { createRoute(it.value) }

            val newRoutes = newRoutesMap.values
            val newRouteStops = newRoutes.flatMap { it.routeStops }
            val newPhysicalStops = newRouteStops.map { it.physicalStop }
            physicalStopJpaRepository.saveAll(newPhysicalStops)
            routeJpaRepository.saveAll(newRoutes)
            routeStopJpaRepository.saveAll(newRouteStops)
            entityManager.flush()

            for (journey in currentPage.content) {
                journeyPatternJpaRepository.setRouteForJourneyPatternById(
                    journey.lineVersionId,
                    journey.patternNumber,
                    newRoutesMap[journey.relationalId]!!.relationalId!!,
                )
            }
        } } while (currentPage?.hasNext() ?: false)
    }
}
