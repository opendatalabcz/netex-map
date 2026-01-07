package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class TimetableStopId(val value: String)

data class TimetableStop(
    val stopId: TimetableStopId,
    val name: String,
) {
}
