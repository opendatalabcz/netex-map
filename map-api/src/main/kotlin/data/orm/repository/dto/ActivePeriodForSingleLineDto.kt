package cz.cvut.fit.gaierda1.data.orm.repository.dto

import java.time.Instant

data class ActivePeriodForSingleLineDto(
    val fromDate: Instant,
    val toDate: Instant,
)