package cz.cvut.fit.gaierda1.presentation.model

import java.time.OffsetDateTime

data class HttpLineVersion(
    val relationalId: Long?,
    val externalId: String,
    val publicCode: String,
    val name: String,
    val shortName: String,
    val transportMode: String,
    val validFrom: OffsetDateTime,
    val validTo: OffsetDateTime,
    val isDetour: Boolean,
    val activePeriods: List<HttpActivePeriod>,
) {
}
