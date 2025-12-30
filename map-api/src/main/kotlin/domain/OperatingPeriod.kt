package cz.cvut.fit.gaierda1.domain

import java.time.LocalDateTime
import java.time.ZoneId

data class OperatingPeriod(
    val timeZone: ZoneId,
    val fromDate: LocalDateTime,
    val toDate: LocalDateTime,
    val validDays: List<Boolean>,
){
}
