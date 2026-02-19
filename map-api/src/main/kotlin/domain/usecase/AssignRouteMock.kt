package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.model.BoundingBox
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import cz.cvut.fit.gaierda1.domain.model.Point
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId
import cz.cvut.fit.gaierda1.domain.model.RouteStop
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import java.time.LocalTime
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class AssignRouteMock {
    companion object {
        private val CZ_BBOX = BoundingBox(Point(12.6296776, 50.7374067), Point(18.1876545, 49.0192903))
        private const val KILOMETER_TO_DEGREE = 0.008_983
        private const val AVG_STEP_LENGTH = KILOMETER_TO_DEGREE / 20
        private const val AVG_SPEED_DPM = 50.0 * KILOMETER_TO_DEGREE / 60.0
    }

    private fun interpolate(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    private fun randomPoint(): Point = Point(
        interpolate(CZ_BBOX.topLeft.longitude, CZ_BBOX.bottomRight.longitude, Random.nextDouble()),
        interpolate(CZ_BBOX.topLeft.latitude, CZ_BBOX.bottomRight.latitude, Random.nextDouble())
    )

    private fun angleBetween(a: Point, b: Point): Double {
        val dx = b.longitude - a.longitude
        val dy = b.latitude - a.latitude
        return atan2(dy, dx)
    }

    private fun stopDistancesPrefixSum(stops: List<ScheduledStop>) = stops
        .fold<ScheduledStop, Pair<ArrayList<Double>, LocalTime>?>(null) { acc, curStop ->
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

    fun assignRoute(journey: Journey): Journey {
        val centerOfMass = randomPoint()
        var currentPoint = randomPoint()
        var angle = (Random.nextDouble() * 2 - 1.0) * PI
        val javaRandom = java.util.Random()
        val path = mutableListOf(currentPoint)
        val routeStops = mutableListOf<RouteStop>()

        val distancePrefixSum = stopDistancesPrefixSum(journey.schedule)
        var cumulativeDistance = 0.0
        journey.schedule.forEachIndexed { idx, scheduledStop ->
            while (cumulativeDistance < distancePrefixSum[idx]) {
                val stepLength = javaRandom.nextExponential() * AVG_STEP_LENGTH
                val nextPoint = Point(currentPoint.longitude + stepLength * cos(angle), currentPoint.latitude + stepLength * sin(angle))
                path.add(nextPoint)
                val angleMean = interpolate(angle, angleBetween(centerOfMass, nextPoint), 0.1)
                angle = javaRandom.nextGaussian(angleMean, 0.6)
                currentPoint = nextPoint
                cumulativeDistance += stepLength
            }
            routeStops.add(RouteStop(
                physicalStop = PhysicalStop(
                    stopId = PhysicalStopId(UUID.randomUUID().toString()),
                    name = "random",
                    position = currentPoint,
                    tags = emptyMap(),
                ),
                pointSequenceIndex = path.lastIndex,
            ))
        }

        return journey.copy(route = Route(
            routeId = RouteId(UUID.randomUUID().toString()),
            pointSequence = path,
            routeStops = routeStops,
        ))
    }
}
