package cz.cvut.fit.gaierda1.domain.model

import java.time.LocalTime

data class ScheduledStop(
    val name: String,
    val stopOnRequest: Boolean,
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
}
