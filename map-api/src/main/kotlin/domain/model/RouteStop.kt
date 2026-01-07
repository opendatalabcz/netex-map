package cz.cvut.fit.gaierda1.domain.model

data class RouteStop(
    val physicalStop: PhysicalStop,
    val pointSequenceIndex: Int,
) {
}
