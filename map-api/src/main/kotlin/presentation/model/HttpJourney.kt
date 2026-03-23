package cz.cvut.fit.gaierda1.presentation.model

import java.time.LocalTime
import java.time.ZoneId

data class HttpJourney(
    val relationalId: Long?,
    val externalId: String,
    val lineVersion: HttpLineVersion,
    val journeyPatternId: String,
    val schedule: List<HttpScheduledStop>,
    val operatingPeriod: HttpOperatingPeriod,
    val route: HttpRoute?,
    val nextDayFirstStopIndex: Int?,
    val beginTime: LocalTime,
    val endTime: LocalTime,
    val timezone: ZoneId,
) {
}
