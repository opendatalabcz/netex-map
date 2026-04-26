package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.domain.port.RoutingServicePort
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase.Companion.distanceBetweenPoints
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase.Companion.externalIdFromWaypoints
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component

@Component
class CalculateRoutesFromWaypoints(
    private val routingServicePort: RoutingServicePort,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
): CalculateRoutesFromWaypointsUseCase {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun stopExternalIdsMatches(
        waypoints: List<PhysicalStop>,
        routeStops: List<RouteStop>,
    ): Boolean {
        if (waypoints.size != routeStops.size) return false
        return waypoints.zip(routeStops).all { (waypoint, routeStop) ->
            waypoint.externalId == routeStop.physicalStop.externalId
        }
    }

    private fun findExistingRoute(
        routeExternalId: String,
        waypoints: List<PhysicalStop>,
        cache: RouteCalculationCache,
    ): Route? {
        val routesFromCache = cache.findRoutes(routeExternalId)
        if (routesFromCache.isNotEmpty()) {
            for (route in routesFromCache) {
                if (stopExternalIdsMatches(waypoints, route.routeStops)) return route
            }
        }
        val fromDbRoutes = routeJpaRepository.findAllByExternalId(routeExternalId)
        if (fromDbRoutes.isNotEmpty()) {
            for (route in fromDbRoutes) {
                if (stopExternalIdsMatches(waypoints, route.routeStops)) {
                    cache.addRoute(route)
                    route.routeStops.forEach { cache.addPhysicalStop(it.physicalStop) }
                    return route
                }
            }
        }
        return null
    }

    private fun resolvePhysicalStops(
        waypoints: List<PhysicalStop>,
        cache: RouteCalculationCache,
    ): List<PhysicalStop> {
        val result = Array<PhysicalStop?>(waypoints.size) { null }
        val indexedWaypoints = waypoints.withIndex()
        val unresolvedWaypointExternalIds = mutableListOf<String>()
        for ((idx, waypoint) in indexedWaypoints) {
            result[idx] = cache.findPhysicalStop(waypoint.externalId)
            if (result[idx] == null) {
                if (waypoint.relationalId != null) {
                    result[idx] = waypoint
                    cache.addPhysicalStop(waypoint)
                    continue
                }
                unresolvedWaypointExternalIds.add(waypoint.externalId)
            }
        }
        if (unresolvedWaypointExternalIds.isNotEmpty()) {
            val stopsFromDb = physicalStopJpaRepository
                .findAllByExternalIds(unresolvedWaypointExternalIds)
                .associateBy(PhysicalStop::externalId)
            for ((idx, waypoint) in indexedWaypoints) {
                if (result[idx] != null) continue
                result[idx] = stopsFromDb[waypoint.externalId] ?: waypoint
                cache.addPhysicalStop(result[idx]!!)
            }
        }
        return result.filterNotNull()
    }

    override fun calculateRouteFromWaypoints(
        waypoints: List<PhysicalStop>,
        cache: RouteCalculationCache?,
    ): Route? {
        if (waypoints.size < 2) return null
        val usedCache = cache ?: RouteCalculationCache()
        val routeExternalId = externalIdFromWaypoints(waypoints)

        val existingRoute = findExistingRoute(routeExternalId, waypoints, usedCache)
        if (existingRoute != null) return existingRoute

        val routeFromService = routingServicePort
            .getRouteForPoints(waypoints.map { it.position.coordinate })
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
        val resolvedPhysicalStops = resolvePhysicalStops(waypoints, usedCache)
        routeStops.addAll(waypoints.mapIndexed { idx, _ ->
            RouteStop(
                stopId = RouteStopId(route.relationalId, idx),
                route = route,
                physicalStop = resolvedPhysicalStops[idx],
                routeFraction = if (cumulativeDistance == 0.0) 0.0 else waypointDistances[idx] / cumulativeDistance,
            )
        })
        usedCache.addRoute(route)
        return route
    }
}