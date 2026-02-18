package cz.cvut.fit.gaierda1.data.http.model

import java.time.ZonedDateTime

data class HttpLineVersion(
    val lineId: String,
    val publicCode: String,
    val name: String,
    val shortName: String,
    val transportMode: String,
    val validFrom: ZonedDateTime,
    val validTo: ZonedDateTime,
    val isDetour: Boolean,
) {
}
