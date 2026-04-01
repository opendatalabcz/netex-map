package cz.cvut.fit.gaierda1.data.orm.repository.dto.wall

import java.time.Instant

data class ActivePeriodWallDto(
    val fromDate: Instant,
    val toDate: Instant,
)
