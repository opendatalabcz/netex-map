package cz.cvut.fit.gaierda1.data.orm.repository.dto.wall

import java.time.LocalTime

data class ScheduledStopWallDto(
    val journeyId: Long,
    val stopOrder: Int,
    val name: String,
    val stopOnRequest: Boolean,
    val arrival: LocalTime?,
    val departure: LocalTime?,
)
