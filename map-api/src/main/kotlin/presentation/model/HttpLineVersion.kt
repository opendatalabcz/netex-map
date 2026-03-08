package cz.cvut.fit.gaierda1.presentation.model

import java.time.ZonedDateTime

data class HttpLineVersion(
    val relationalId: Long?,
    val externalId: String,
    val publicCode: String,
    val name: String,
    val shortName: String,
    val transportMode: String,
    val validFrom: ZonedDateTime,
    val validTo: ZonedDateTime,
    val isDetour: Boolean,
) {
}
