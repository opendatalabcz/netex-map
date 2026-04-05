package cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails

data class JourneyPatternStopJourneyDetailsDto(
    val stopOrder: Int,
    val tariffOrder: Int,
    val distanceToNextStop: Double,
    val forBoarding: Boolean,
    val forAlighting: Boolean,
    val requiresOrdering: Boolean,
    val stopOnRequest: Boolean,
)
