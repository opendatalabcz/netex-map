package cz.cvut.fit.gaierda1.domain

@JvmInline
value class JourneyPatternLineLocalId(val value: String)

data class JourneyPattern(
    val lineLocalId: JourneyPatternLineLocalId,
    val stops: List<JourneyStop>,
    val route: Route,
    val line: Line,
) {
}
