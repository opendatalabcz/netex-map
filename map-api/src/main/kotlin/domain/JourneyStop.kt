package cz.cvut.fit.gaierda1.domain

data class JourneyStop(
    val stop: TimetableStop,
    val order: Int,
    val routeIndex: Int
) {
}
