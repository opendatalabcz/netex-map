package cz.cvut.fit.gaierda1.data.orm.repository.dto

import java.time.Instant

data class ActivePeriodDto(
    val lineVersionId: Long,
    val fromDate: Instant,
    val toDate: Instant,
)
