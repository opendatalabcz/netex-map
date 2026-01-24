package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class LineId(val value: String)

data class LineVersion(
    val lineId: LineId,
    val publicCode: String,
    val name: String,
    val shortName: String,
    val transportMode: String, // TODO make enum
    val validIn: DateRange,
    val isDetour: Boolean,
) {
}
