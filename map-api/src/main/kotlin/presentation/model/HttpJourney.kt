package cz.cvut.fit.gaierda1.presentation.model

data class HttpJourney(
    val relationalId: Long?,
    val externalId: String,
    val lineVersion: HttpLineVersion,
    val journeyPatternId: String,
    val schedule: List<HttpScheduledStop>,
    val operatingPeriods: List<HttpOperatingPeriod>,
    val route: HttpRoute?,
    val nextDayFirstStopIndex: Int?,
) {
}
