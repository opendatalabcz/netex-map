package cz.cvut.fit.gaierda1.data.orm.repository.dto

import java.time.LocalTime

data class ScheduledStopDto(
    val journeyId: Long,
    val stopOrder: Int,
    val arrival: LocalTime?,
    val departure: LocalTime?,
)