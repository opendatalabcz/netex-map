package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class JourneyId(val value: String)

data class Journey(
    val journeyId: JourneyId,
    val lineVersion: LineVersion,
    val schedule: List<ScheduledStop>,
    val operatingPeriod: OperatingPeriod,
    val route: Route?,
) {
}
