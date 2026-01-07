package cz.cvut.fit.gaierda1.domain.model

import java.time.LocalTime

data class ScheduledStop(
    val timetableStop: TimetableStop,
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
}
