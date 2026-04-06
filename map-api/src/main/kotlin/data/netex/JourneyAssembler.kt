package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPattern
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import org.rutebanken.netex.model.CateringFacilityEnumeration
import org.rutebanken.netex.model.LuggageCarriageEnumeration
import org.rutebanken.netex.model.MobilityFacilityEnumeration
import org.rutebanken.netex.model.ReservationEnumeration
import org.rutebanken.netex.model.ServiceFacilitySet
import org.rutebanken.netex.model.ServiceJourney
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId

@Component
class JourneyAssembler(
    private val journeyJpaRepository: JourneyJpaRepository,
) {
    fun assembleJourneys(
        registry: NetexFileRegistry,
        journeyPatterns: Map<String, JourneyPattern>,
        operatingPeriods: Map<String, OperatingPeriod>,
        parseCache: TimetableParseResult,
    ): Map<String, Journey> {
        val journeys = mutableMapOf<String, Journey>()
        val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
        for (journey in registry.serviceJourneyRegistry.values) {
            val journeyPatternKey = journey.journeyPatternRef.value.ref
            val journeyPattern = journeyPatterns[journeyPatternKey]
            checkNotNull(journeyPattern) { "Journey pattern $journeyPatternKey not found" }
            val lineVersion = journeyPattern.lineVersion

            val fromCache = parseCache.findJourney(
                journeyNumber = journey.name.value,
                publicCode = lineVersion.publicCode,
                isDetour = lineVersion.isDetour,
                validFrom = lineVersion.validFrom,
                validTo = lineVersion.validTo,
            )
            if (fromCache != null) {
                journeys[journey.id] = fromCache
                continue
            }

            val journeyId = journeyJpaRepository.findIdByJourneyNumberAndLinePublicCodeAndValidRangeAndDetour(
                journeyNumber = journey.name.value,
                publicCode = lineVersion.publicCode,
                validFrom = lineVersion.validFrom,
                validTo = lineVersion.validTo,
                isDetour = lineVersion.isDetour,
            )
            val assembledJourney = assembleJourney(
                journey = journey,
                journeyPattern = journeyPattern,
                journeyPatternKey = journeyPatternKey,
                registry = registry,
                operatingPeriods = operatingPeriods,
                timezone = zoneId,
            )
            journeyId.ifPresent {
                assembledJourney.relationalId = it
            }
            parseCache.addJourney(assembledJourney)
            journeys[journey.id] = assembledJourney
        }
        return journeys
    }

    private fun assembleJourney(
        journey: ServiceJourney,
        journeyPattern: JourneyPattern,
        journeyPatternKey: String,
        registry: NetexFileRegistry,
        operatingPeriods: Map<String, OperatingPeriod>,
        timezone: ZoneId,
    ): Journey {
        val schedule = mutableListOf<ScheduledStop>()
        val operatingPeriod = linkOperatingPeriod(journey, registry, operatingPeriods)
        val serviceFacilitySet = journey.facilities?.serviceFacilitySetRefOrServiceFacilitySet?.let {
            if (it.size > 1) error("Expected at most one service facility set, found ${it.size}")
            it.firstOrNull() as? ServiceFacilitySet
        }
        val assembledJourney = Journey(
            relationalId = null,
            journeyNumber = journey.name.value,
            schedule = schedule,
            operatingPeriod = operatingPeriod,
            nextDayFirstStopIndex = null,
            timezone = timezone,
            beginTime = LocalTime.MIN,
            endTime = LocalTime.MIN,
            journeyPattern = journeyPattern,
            requiresOrdering = journey.flexibleServiceProperties?.isCancellationPossible ?: false,
            baggageStorage = serviceFacilitySet?.luggageCarriageFacilityList?.contains(
                LuggageCarriageEnumeration.BAGGAGE_STORAGE) ?: false,
            cyclesAllowed = serviceFacilitySet?.luggageCarriageFacilityList?.contains(
                LuggageCarriageEnumeration.CYCLES_ALLOWED) ?: false,
            lowFloorAccess = serviceFacilitySet?.mobilityFacilityList?.contains(
                MobilityFacilityEnumeration.LOW_FLOOR) ?: false,
            reservationCompulsory = serviceFacilitySet?.serviceReservationFacilityList?.contains(
                ReservationEnumeration.RESERVATIONS_COMPULSORY) ?: false,
            reservationPossible = serviceFacilitySet?.serviceReservationFacilityList?.contains(
                ReservationEnumeration.RESERVATIONS_POSSIBLE) ?: false,
            snacksOnBoard = serviceFacilitySet?.cateringFacilityList?.contains(
                CateringFacilityEnumeration.SNACKS) ?: false,
            unaccompaniedMinorAssistance = serviceFacilitySet?.mobilityFacilityList?.contains(
                MobilityFacilityEnumeration.UNACCOMPANIED_MINOR_ASSISTANCE) ?: false,
        )
        val patternRegistryValue = registry.serviceJourneyPatternRegistry[journeyPatternKey]
        checkNotNull(patternRegistryValue) { "Journey pattern $journeyPatternKey not found" }
        schedule.addAll(assembleScheduledStops(
            serviceJourney = journey,
            assembledJourney = assembledJourney,
            registry = registry,
        ))
        assembledJourney.beginTime = schedule.first().run { arrival ?: departure!! }
        assembledJourney.endTime = schedule.last().run { departure ?: arrival!! }
        return assembledJourney
    }
    
    private fun assembleScheduledStops(
        serviceJourney: ServiceJourney,
        assembledJourney: Journey,
        registry: NetexFileRegistry,
    ): List<ScheduledStop> {
        val scheduledStops = mutableListOf<ScheduledStop>()
        for (timetabledPassingTime in serviceJourney.passingTimes.timetabledPassingTime) {
            val stopPointInJourneyPatternKey = timetabledPassingTime.pointInJourneyPatternRef.value.ref
            val stopPointInJourneyPattern = registry.stopPointInJourneyPatternRegistry[stopPointInJourneyPatternKey]
            checkNotNull(stopPointInJourneyPattern) { "Stop point in journey pattern $stopPointInJourneyPatternKey not found" }

            scheduledStops.add(ScheduledStop(
                stopId = ScheduledStopId(assembledJourney.relationalId, stopPointInJourneyPattern.order.toInt()),
                journey = assembledJourney,
                arrival = timetabledPassingTime.arrivalTime,
                departure = timetabledPassingTime.departureTime,
            ))
        }
        scheduledStops.sortBy { it.stopId.stopOrder }
        return scheduledStops
    }
    
    private fun linkOperatingPeriod(
        journey: ServiceJourney,
        registry: NetexFileRegistry,
        operatingPeriods: Map<String, OperatingPeriod>,
    ): OperatingPeriod {
        val dayTypeRef = journey.dayTypes.dayTypeRef.firstOrNull()
        checkNotNull(dayTypeRef) { "Journey ${journey.id} has no day type" }

        val dayTypeKey = dayTypeRef.value.ref
        val dayTypeAssignment = registry.dayTypeAssignmentRegistryByDayTypeId[dayTypeKey]
        checkNotNull(dayTypeAssignment) { "Day type assignment for $dayTypeKey not found" }

        val operatingPeriodKey = dayTypeAssignment.operatingPeriodRef.value.ref
        val operatingPeriod = operatingPeriods[operatingPeriodKey]
        checkNotNull(operatingPeriod) { "Operating period $operatingPeriodKey not found" }

        return operatingPeriod
    }
}
