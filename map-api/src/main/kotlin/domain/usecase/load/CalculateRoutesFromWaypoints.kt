package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsStopParseResult
import cz.cvut.fit.gaierda1.domain.port.RoutingServicePort
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.math.cos
import kotlin.math.sqrt

@Component
class CalculateRoutesFromWaypoints(
    private val routingServicePort: RoutingServicePort,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
): CalculateRoutesFromWaypointsUseCase {
    companion object {
        private const val EARTH_RADIUS = 6_371_000.0
    }

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun stopNamesMatches(
        waypoints: List<JrUtilGtfsStopParseResult>,
        routeStops: List<RouteStop>,
    ): Boolean {
        if (waypoints.size != routeStops.size) return false
        return waypoints.zip(routeStops).all { (waypoint, routeStop) ->
            waypoint.name == routeStop.physicalStop.name
        }
    }

    private fun findExistingRoute(
        routeExternalId: String,
        waypoints: List<JrUtilGtfsStopParseResult>,
        cache: RouteCalculationCache,
    ): Route? {
        val routesFromCache = cache.findRoutes(routeExternalId)
        if (routesFromCache.isNotEmpty()) {
            for (route in routesFromCache) {
                if (stopNamesMatches(waypoints, route.routeStops)) return route
            }
        }
        val fromDbRoutes = routeJpaRepository.findAllByExternalId(routeExternalId)
        if (fromDbRoutes.isNotEmpty()) {
            for (route in fromDbRoutes) {
                if (stopNamesMatches(waypoints, route.routeStops)) {
                    cache.addRoute(route)
                    return route
                }
            }
        }
        return null
    }

    private fun distanceBetweenPoints(a: Coordinate, b: Coordinate): Double {
        val cosLat = cos(a.y * Math.PI / 180)
        val dLonRad = (b.x - a.x) * Math.PI / 180 * cosLat
        val dLatRad = (b.y - a.y) * Math.PI / 180
        return EARTH_RADIUS * sqrt(dLatRad * dLatRad + dLonRad * dLonRad)
    }

    override fun calculateRouteFromWaypoints(
        waypoints: List<JrUtilGtfsStopParseResult>,
        cache: RouteCalculationCache?,
    ): Route? {
        if (waypoints.size < 2) return null
        val usedCache = cache ?: RouteCalculationCache()
        val routeExternalId = Base64.encode(
            MessageDigest.getInstance("SHA-256")
                .digest(waypoints.joinToString("|") { it.name }.toByteArray())
        )

        val existingRoute = findExistingRoute(routeExternalId, waypoints, usedCache)
        if (existingRoute != null) return existingRoute

        val routeFromService = routingServicePort
            .getRouteForPoints(waypoints.map { it.coordinate })
            ?: return null

        var waypointIndex = 1
        var cumulativeDistance = 0.0
        var prevRoutePoint = routeFromService.waypoints.first()
        val waypointDistances = Array(routeFromService.waypoints.size) { 0.0 }
        while (waypointIndex < routeFromService.waypoints.size
            && routeFromService.waypoints[waypointIndex] == routeFromService.waypoints[waypointIndex - 1]
        ) {
            waypointIndex++
        }
        for (pointIndex in 1 until routeFromService.route.size) {
            val routePoint = routeFromService.route[pointIndex]
            cumulativeDistance += distanceBetweenPoints(prevRoutePoint, routePoint)
            if (waypointIndex < routeFromService.waypoints.size
                && routePoint == routeFromService.waypoints[waypointIndex]
            ) {
                waypointDistances[waypointIndex] = cumulativeDistance
                waypointIndex++
                while (waypointIndex < routeFromService.waypoints.size
                    && routeFromService.waypoints[waypointIndex] == routeFromService.waypoints[waypointIndex - 1]
                ) {
                    waypointDistances[waypointIndex] = cumulativeDistance
                    waypointIndex++
                }
            }
            prevRoutePoint = routePoint
        }
        waypointDistances[routeFromService.waypoints.size - 1] = cumulativeDistance
        val routeStops = mutableListOf<RouteStop>()
        val routePoints =
            if (routeFromService.route.size >= 2) routeFromService.route.toTypedArray()
            else Array(2) { routeFromService.route.first() }
        val route =  Route(
            relationalId = null,
            externalId = routeExternalId,
            pointSequence = geometryFactory.createLineString(routePoints),
            routeStops = routeStops,
            totalDistance = cumulativeDistance,
        )
        routeStops.addAll(waypoints.mapIndexed { idx, stop ->
            var physicalStop = usedCache.findPhysicalStop(stop.name)
            if (physicalStop == null) {
                physicalStop = physicalStopJpaRepository
                    .findByExternalId(stop.name)
                    .orElseGet { PhysicalStop(
                        relationalId = null,
                        externalId = stop.name,
                        name = stop.name,
                        position = geometryFactory.createPoint(stop.coordinate),
                        tags = emptyMap(),
                    ) }
                usedCache.addPhysicalStop(physicalStop)
            }
            RouteStop(
                stopId = RouteStopId(route.relationalId, idx),
                route = route,
                physicalStop = physicalStop,
                routeFraction = if (cumulativeDistance == 0.0) 0.0 else waypointDistances[idx] / cumulativeDistance,
            )
        })
        usedCache.addRoute(route)
        return route
    }
}