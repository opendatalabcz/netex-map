package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class JourneyId(val value: String)

@JvmInline
value class JourneyPatternId(val value: String)

data class Journey(
    val journeyId: JourneyId,
    val lineVersion: LineVersion,
    val journeyPatternId: JourneyPatternId,
    val schedule: List<ScheduledStop>,
    val operatingPeriods: List<OperatingPeriod>,
    val route: Route?,
) {
}
