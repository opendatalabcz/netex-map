package cz.cvut.fit.gaierda1.data.http.model

data class HttpJourney(
    val journeyId: String,
    val lineVersion: HttpLineVersion,
    val journeyPatternId: String,
    val schedule: List<HttpScheduledStop>,
    val operatingPeriods: List<HttpOperatingPeriod>,
    val route: HttpRoute?,
    val nextDayFirstStopIndex: Int?,
) {
}
