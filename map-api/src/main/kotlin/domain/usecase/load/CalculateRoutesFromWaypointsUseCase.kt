package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route

interface CalculateRoutesFromWaypointsUseCase {
    fun calculateRouteFromWaypoints(
        waypoints: List<PhysicalStop>,
        cache: RouteCalculationCache? = null,
    ): Route?
}
