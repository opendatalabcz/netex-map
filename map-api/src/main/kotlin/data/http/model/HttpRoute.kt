package cz.cvut.fit.gaierda1.data.http.model

data class HttpRoute(
    val routeId: String,
    val pointSequence: List<Array<Double>>,
    val routeStops: List<HttpRouteStop>,
) {
}
