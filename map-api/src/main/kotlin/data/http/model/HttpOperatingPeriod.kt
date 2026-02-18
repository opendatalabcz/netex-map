package cz.cvut.fit.gaierda1.data.http.model

import java.time.LocalDateTime

class HttpOperatingPeriod(
    val timezone: String,
    val fromDate: LocalDateTime,
    val toDate: LocalDateTime,
    val validDays: String,
) {
}
