package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route

class RouteCalculationCache {
    private val _routes = mutableMapOf<String, MutableList<Route>>()
    private val _physicalStops = mutableMapOf<String, PhysicalStop>()
    
    val routes: List<Route>
        get() = _routes.values.flatMap { it }
    val physicalStops: List<PhysicalStop>
        get() = _physicalStops.values.toList()
    
    fun addRoute(route: Route) {
        val routesForExternalKey = _routes.getOrPut(route.externalId) { mutableListOf() }
        routesForExternalKey.add(route)
    }
    
    fun addPhysicalStop(physicalStop: PhysicalStop) {
        _physicalStops[physicalStop.externalId] = physicalStop
    }

    fun findRoutes(externalId: String): List<Route> {
        return _routes[externalId]?.toList() ?: emptyList()
    }

    fun findPhysicalStop(externalId: String): PhysicalStop? {
        return _physicalStops[externalId]
    }
}
