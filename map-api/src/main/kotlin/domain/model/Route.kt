package cz.cvut.fit.gaierda1.domain.model

data class Route(
    val pointSequence: List<Point>,
    val stops: List<RouteStop>,
) {
}
