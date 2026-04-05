package cz.cvut.fit.gaierda1.data.orm.repository.dto

data class StopDto(
    val relationalId: Long,
    val name: String,
    val bistro: Boolean,
    val borderCrossing: Boolean,
    val displaysForVisuallyImpaired: Boolean,
    val lowFloorAccess: Boolean,
    val parkAndRidePark: Boolean,
    val suitableForHeavilyDisabled: Boolean,
    val toilet: Boolean,
    val wheelChairAccessToilet: Boolean,
    val otherTransportModes: String?,
)