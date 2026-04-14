package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.model.LineType
import cz.cvut.fit.gaierda1.data.orm.model.TransportMode
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatorJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.TariffStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.WithinRegionTransportBanJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.StopDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.TariffStopForSingleLineDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails.JourneyPatternStopJourneyDetailsDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails.ScheduledStopJourneyDetailsDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails.TransportBanJourneyDetailsDto
import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneyDetailsUseCase.*
import org.springframework.stereotype.Component

@Component
class GetJourneyDetails(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val journeyPatternStopJpaRepository: JourneyPatternStopJpaRepository,
    private val tariffStopJpaRepository: TariffStopJpaRepository,
    private val stopJpaRepository: StopJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val withinRegionTransportBanJpaRepository: WithinRegionTransportBanJpaRepository,
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatorJpaRepository: OperatorJpaRepository,
): GetJourneyDetailsUseCase {
    override fun getJourneyDetails(journeyId: Long): JourneyDetails? {
        val journey = journeyJpaRepository.findDetailsDtoByJourneyId(journeyId).orElse(null) ?: return null
        val lineVersion = lineVersionJpaRepository.findJourneyDetailsDtoById(journey.lineVersionId).orElse(null) ?: return null
        val operator = operatorJpaRepository.findDtoByOperatorId(lineVersion.operatorId).orElse(null) ?: return null
        val scheduledStops = scheduledStopJpaRepository
            .findAllJourneyDetailsDtoByJourneyId(journeyId)
            .sortedBy(ScheduledStopJourneyDetailsDto::stopOrder)
        val journeyPatternStops = journeyPatternStopJpaRepository
            .findAllJourneyDetailsDtoByLineVersionIdAndPatternNumber(journey.lineVersionId, journey.patternNumber)
            .sortedBy(JourneyPatternStopJourneyDetailsDto::stopOrder)
        val withinRegionTransportBans = withinRegionTransportBanJpaRepository
            .findAllJourneyDetailsDtoByLineVersionIdAndPatternNumber(journey.lineVersionId, journey.patternNumber)
            .groupBy(TransportBanJourneyDetailsDto::banGroupNumber)
            .mapValues { (_, bans) -> bans.map(TransportBanJourneyDetailsDto::stopOrder) }
        val tariffStops = tariffStopJpaRepository
            .findAllDtoForSingleLineByLineVersionId(journey.lineVersionId)
            .sortedBy(TariffStopForSingleLineDto::tariffOrder)
        val stops = stopJpaRepository
            .findAllDtoByStopIds(tariffStops.map(TariffStopForSingleLineDto::stopId))
            .associateBy(StopDto::relationalId)

        val reconstructedStops = scheduledStops.map { scheduledStop ->
            val journeyPatternStop = journeyPatternStops[scheduledStop.stopOrder]
            val tariffStop = tariffStops[journeyPatternStop.tariffOrder]
            val stop = stops[tariffStop.stopId]!!
            JourneyDetailsScheduledStop(
                arrival = scheduledStop.arrival,
                departure = scheduledStop.departure,
                distanceToNextStop = journeyPatternStop.distanceToNextStop,
                forBoarding = journeyPatternStop.forBoarding,
                forAlighting = journeyPatternStop.forAlighting,
                requiresOrdering = journeyPatternStop.requiresOrdering,
                stopOnRequest = journeyPatternStop.stopOnRequest,
                tariffZone = tariffStop.tariffZone,
                name = stop.name,
                bistro = stop.bistro,
                borderCrossing = stop.borderCrossing,
                displaysForVisuallyImpaired = stop.displaysForVisuallyImpaired,
                lowFloorAccess = stop.lowFloorAccess,
                parkAndRidePark = stop.parkAndRidePark,
                suitableForHeavilyDisabled = stop.suitableForHeavilyDisabled,
                toilet = stop.toilet,
                wheelChairAccessToilet = stop.wheelChairAccessToilet,
                otherTransportModes = stop.otherTransportModes,
            )
        }

        val reconstructedLineVersion = JourneyDetailsLineVersion(
            relationalId = lineVersion.relationalId,
            publicCode = lineVersion.publicCode,
            name = lineVersion.name,
            shortName = lineVersion.shortName,
            transportMode = TransportMode.fromShortCode(lineVersion.transportMode),
            lineType = LineType.fromJdfCode(lineVersion.lineType),
            isDetour = lineVersion.isDetour,
            operator = operator,
        )

        return JourneyDetails(
            relationalId = journeyId,
            routeId = journey.routeId,
            stops = reconstructedStops,
            transportBans = withinRegionTransportBans.values.toList(),
            lineVersion = reconstructedLineVersion,
            requiresOrdering = journey.requiresOrdering,
            baggageStorage = journey.baggageStorage,
            cyclesAllowed = journey.cyclesAllowed,
            lowFloorAccess = journey.lowFloorAccess,
            reservationCompulsory = journey.reservationCompulsory,
            reservationPossible = journey.reservationPossible,
            snacksOnBoard = journey.snacksOnBoard,
            unaccompaniedMinorAssistance = journey.unaccompaniedMinorAssistance,
        )
    }
}