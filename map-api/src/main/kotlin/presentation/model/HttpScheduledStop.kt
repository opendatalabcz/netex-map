package cz.cvut.fit.gaierda1.presentation.model

import java.time.LocalTime

data class HttpScheduledStop(
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
}
