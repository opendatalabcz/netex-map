package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class RouteId(val value: String)

data class Route(
    val routeId: RouteId,
    val pointSequence: List<Point>,
    val routeStops: List<RouteStop>,
) {
}
