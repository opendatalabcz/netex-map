package cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails

data class JourneyDetailsDto(
    val lineVersionId: Long,
    val patternNumber: Int,
    val routeId: Long?,
    val requiresOrdering: Boolean,
    val baggageStorage: Boolean,
    val cyclesAllowed: Boolean,
    val lowFloorAccess: Boolean,
    val reservationCompulsory: Boolean,
    val reservationPossible: Boolean,
    val snacksOnBoard: Boolean,
    val unaccompaniedMinorAssistance: Boolean,
)
