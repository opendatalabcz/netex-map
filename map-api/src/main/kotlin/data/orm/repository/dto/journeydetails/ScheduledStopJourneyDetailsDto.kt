package cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails

import java.time.LocalTime

data class ScheduledStopJourneyDetailsDto(
    val stopOrder: Int,
    val arrival: LocalTime?,
    val departure: LocalTime?,
)
