package cz.cvut.fit.gaierda1.domain.port

import org.locationtech.jts.geom.Coordinate

interface RoutingServicePort {
    data class RoutingResponse(
        val route: List<Coordinate>,
        val distance: Double,
        val waypoints: List<Coordinate>,
    )
    fun getRouteForPoints(points: List<Coordinate>): RoutingResponse?
}
