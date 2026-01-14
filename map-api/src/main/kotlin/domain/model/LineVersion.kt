package cz.cvut.fit.gaierda1.domain.model

data class LineVersion(
    val line: Line,
    val name: String,
    val shortName: String,
    val transportMode: String, // TODO make enum
    val validIn: DateRange,
    val isDetour: Boolean,
) {
}
