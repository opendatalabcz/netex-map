package cz.cvut.fit.gaierda1.data.orm.repository.dto.wall

import java.time.LocalDateTime

data class OperatingPeriodWallDto(
    val relationalId: Long,
    val fromDate: LocalDateTime,
    val toDate: LocalDateTime,
    val validDays: Array<Boolean>,
)
