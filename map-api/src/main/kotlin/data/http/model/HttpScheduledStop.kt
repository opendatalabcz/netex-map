package cz.cvut.fit.gaierda1.data.http.model

import java.time.LocalTime

data class HttpScheduledStop(
    val name: String,
    val stopOnRequest: Boolean,
    val arrival: LocalTime?,
    val departure: LocalTime?,
) {
}
