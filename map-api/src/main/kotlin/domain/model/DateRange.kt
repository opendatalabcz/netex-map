package cz.cvut.fit.gaierda1.domain.model

import java.time.LocalDateTime
import java.time.ZoneId

data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
    val timezone: ZoneId,
) {
}
