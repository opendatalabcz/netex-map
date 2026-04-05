package cz.cvut.fit.gaierda1.data.orm.repository.dto.wall

data class JourneyWallDto(
    val relationalId: Long,
    val operatingPeriodId: Long,
    val requiresOrdering: Boolean,
    val baggageStorage: Boolean,
    val cyclesAllowed: Boolean,
    val lowFloorAccess: Boolean,
    val reservationCompulsory: Boolean,
    val reservationPossible: Boolean,
    val snacksOnBoard: Boolean,
    val unaccompaniedMinorAssistance: Boolean,
)
