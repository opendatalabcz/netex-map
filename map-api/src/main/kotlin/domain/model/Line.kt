package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class LineId(val value: String)

data class Line(
    val lineId: LineId,
    val publicCode: String,
) {
}
