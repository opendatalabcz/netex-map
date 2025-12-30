package cz.cvut.fit.gaierda1.domain

import java.time.LocalTime

data class TimetablePassingTime(
    val stop: JourneyStop,
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
}
