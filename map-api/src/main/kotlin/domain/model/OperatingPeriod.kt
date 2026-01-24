package cz.cvut.fit.gaierda1.domain.model

import java.time.LocalDateTime
import java.time.ZoneId

data class OperatingPeriod(
    val timezone: ZoneId,
    val fromDate: LocalDateTime,
    val toDate: LocalDateTime,
    val validDays: List<Boolean>,
) {
}
