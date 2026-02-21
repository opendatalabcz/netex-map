package cz.cvut.fit.gaierda1.data.http.model

import java.time.ZonedDateTime

class HttpOperatingPeriod(
    val fromDate: ZonedDateTime,
    val toDate: ZonedDateTime,
    val validDays: String,
) {
}
