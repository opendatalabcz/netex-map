package cz.cvut.fit.gaierda1.domain

@JvmInline
value class TimetableStopId(val value: String)

data class TimetableStop(
    val stopId: TimetableStopId,
    val location: Point,
    val name: String,
) {
}
