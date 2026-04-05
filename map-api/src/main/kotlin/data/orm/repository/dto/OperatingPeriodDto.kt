package cz.cvut.fit.gaierda1.data.orm.repository.dto

import java.time.LocalDateTime

data class OperatingPeriodDto(
    val relationalId: Long,
    val fromDate: LocalDateTime,
    val toDate: LocalDateTime,
    val validDays: Array<Boolean>,
)