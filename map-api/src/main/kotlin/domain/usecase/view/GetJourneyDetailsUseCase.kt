package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.model.LineType
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatorDto
import java.time.LocalTime

interface GetJourneyDetailsUseCase {
    data class JourneyDetailsScheduledStop(
        val arrival: LocalTime?,
        val departure: LocalTime?,
        val distanceToNextStop: Double,
        val forBoarding: Boolean,
        val forAlighting: Boolean,
        val requiresOrdering: Boolean,
        val stopOnRequest: Boolean,
        val tariffZone: String?,
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
    data class JourneyDetailsLineVersion(
        val relationalId: Long,
        val publicCode: String,
        val name: String,
        val shortName: String,
        val transportMode: String,
        val lineType: LineType,
        val isDetour: Boolean,
        val operator: OperatorDto,
    )
    data class JourneyDetails(
        val relationalId: Long,
        val routeId: Long?,
        val stops: List<JourneyDetailsScheduledStop>,
        val transportBans: List<List<Int>>?,
        val lineVersion: JourneyDetailsLineVersion,
        val requiresOrdering: Boolean,
        val baggageStorage: Boolean,
        val cyclesAllowed: Boolean,
        val lowFloorAccess: Boolean,
        val reservationCompulsory: Boolean,
        val reservationPossible: Boolean,
        val snacksOnBoard: Boolean,
        val unaccompaniedMinorAssistance: Boolean,
    )
    fun getJourneyDetails(journeyId: Long): JourneyDetails?
}