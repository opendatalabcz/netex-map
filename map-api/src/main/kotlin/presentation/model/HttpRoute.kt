package cz.cvut.fit.gaierda1.presentation.model

data class HttpRoute(
    val relationalId: Long?,
    val externalId: String,
    val pointSequence: List<Array<Double>>,
    val routeStops: List<HttpRouteStop>,
    val totalDistance: Double,
) {
}
