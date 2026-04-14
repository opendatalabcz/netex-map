package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsStopParseResult

interface CalculateRoutesFromWaypointsUseCase {
    fun calculateRouteFromWaypoints(
        waypoints: List<JrUtilGtfsStopParseResult>,
        cache: RouteCalculationCache? = null,
    ): Route?
}
