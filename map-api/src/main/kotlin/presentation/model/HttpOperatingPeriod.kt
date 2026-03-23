package cz.cvut.fit.gaierda1.presentation.model

import java.time.LocalDateTime

class HttpOperatingPeriod(
    val fromDate: LocalDateTime,
    val toDate: LocalDateTime,
    val validDays: String,
) {
}
