package cz.cvut.fit.gaierda1.domain

@JvmInline
value class LineId(val value: String)

data class Line(
    val lineId: LineId,
    val name: String,
    val shortName: String,
    val publicCode: String,
    val transportMode: String,
) {
}
