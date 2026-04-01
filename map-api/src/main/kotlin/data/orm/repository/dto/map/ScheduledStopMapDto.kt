package cz.cvut.fit.gaierda1.data.orm.repository.dto.map

import java.time.LocalTime

data class ScheduledStopMapDto(
    val journeyId: Long,
    val stopOrder: Int,
    val arrival: LocalTime?,
    val departure: LocalTime?,
)
