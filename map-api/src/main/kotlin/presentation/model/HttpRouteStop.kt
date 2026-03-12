package cz.cvut.fit.gaierda1.presentation.model

data class HttpRouteStop(
    val physicalStop: HttpPhysicalStop,
    val pointSequenceIndex: Int,
    val distanceToNextStop: Double,
) {
}
