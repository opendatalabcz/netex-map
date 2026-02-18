package cz.cvut.fit.gaierda1.data.http.model

data class HttpRouteStop(
    val physicalStop: HttpPhysicalStop,
    val pointSequenceIndex: Int,
) {
}
