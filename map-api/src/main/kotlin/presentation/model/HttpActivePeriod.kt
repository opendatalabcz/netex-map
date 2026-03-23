package cz.cvut.fit.gaierda1.presentation.model

import java.time.OffsetDateTime

data class HttpActivePeriod(
    val fromDate: OffsetDateTime,
    val toDate: OffsetDateTime,
) {
}
