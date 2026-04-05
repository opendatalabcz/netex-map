package cz.cvut.fit.gaierda1.presentation.model

import java.time.LocalTime
import java.time.ZoneId

data class HttpJourney(
    val relationalId: Long?,
    val journeyNumber: String,
    val lineVersion: HttpLineVersion,
    val route: HttpRoute?,
    val operatingPeriod: HttpOperatingPeriod,
    val schedule: List<HttpScheduledStop>,
    val nextDayFirstStopIndex: Int?,
    val beginTime: LocalTime,
    val endTime: LocalTime,
    val timezone: ZoneId,
) {
}
