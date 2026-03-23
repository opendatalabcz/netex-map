package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId

@Component
class JourneyAssembler(
    private val journeyJpaRepository: JourneyJpaRepository,
) {
    fun assembleJourneys(
        registry: NetexFileRegistry,
        lineVersions: Map<String, LineVersion>,
        operatingPeriods: Map<String, OperatingPeriod>,
    ): Map<String, Journey> {
        val journeys = mutableMapOf<String, Journey>()
        val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
        for (journey in registry.serviceJourneyRegistry.values) {
            val journeyPatternId = journey.journeyPatternRef.value.ref
            val patternRegistryValue = registry.serviceJourneyPatternRegistry[journeyPatternId]
            checkNotNull(patternRegistryValue) { "Journey pattern $journeyPatternId not found" }

            val lineId = patternRegistryValue.serviceJourneyPattern.routeView.lineRef.value.ref
            val lineVersion = lineVersions[lineId]
            checkNotNull(lineVersion) { "Line $lineId not found" }

            journeys[journey.id] = journeyJpaRepository
                .findByExternalIdAndLineIdAndValidRange(
                    externalId = journey.id,
                    lineExternalId = lineVersion.externalId,
                    validFrom = lineVersion.validFrom,
                    validTo = lineVersion.validTo,
                    isDetour = lineVersion.isDetour,
                ).orElseGet { assembleJourney(
                    journey = journey,
                    journeyPatternId = journeyPatternId,
                    lineVersion = lineVersion,
                    registry = registry,
                    stopPointInJourneyPatternRegistry = patternRegistryValue.stopPointInJourneyPatternRegistry,
                    operatingPeriods = operatingPeriods,
                    timezone = zoneId
                ) }
        }
        return journeys
    }

    private fun assembleJourney(
        journey: ServiceJourney,
        journeyPatternId: String,
        lineVersion: LineVersion,
        registry: NetexFileRegistry,
        stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
        operatingPeriods: Map<String, OperatingPeriod>,
        timezone: ZoneId,
    ): Journey {
        val schedule = mutableListOf<ScheduledStop>()
        val operatingPeriod = linkOperatingPeriod(journey, registry, operatingPeriods)
        val savedJourney = Journey(
            relationalId = null,
            externalId = journey.id,
            lineVersion = lineVersion,
            journeyPatternId = journeyPatternId,
            schedule = schedule,
            operatingPeriod = operatingPeriod,
            route = null,
            nextDayFirstStopIndex = null,
            timezone = timezone,
            beginTime = LocalTime.MIN,
            endTime = LocalTime.MIN,
        )
        schedule.addAll(assembleScheduledStops(
            serviceJourney = journey,
            savedJourney = savedJourney,
            stopPointInJourneyPatternRegistry = stopPointInJourneyPatternRegistry,
            scheduledStopPointRegistry = registry.scheduledStopPointRegistry
        ))
        savedJourney.beginTime = schedule.first().run { arrival ?: departure!! }
        savedJourney.endTime = schedule.last().run { departure ?: arrival!! }
        return savedJourney
    }
    
    private fun assembleScheduledStops(
        serviceJourney: ServiceJourney,
        savedJourney: Journey,
        stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
        scheduledStopPointRegistry: Map<String, ScheduledStopPoint>,
    ): List<ScheduledStop> {
        return serviceJourney.passingTimes.timetabledPassingTime
            .map { timetabledPassingTime ->
                val stopPointInJourneyPatternId = timetabledPassingTime.pointInJourneyPatternRef.value.ref
                val stopPointInJourneyPattern = stopPointInJourneyPatternRegistry[stopPointInJourneyPatternId]
                checkNotNull(stopPointInJourneyPattern) { "Stop point in journey pattern $stopPointInJourneyPatternId not found" }

                val scheduledStopPointId = stopPointInJourneyPattern.scheduledStopPointRef.value.ref
                val scheduledStopPoint = scheduledStopPointRegistry[scheduledStopPointId]
                checkNotNull(scheduledStopPoint) { "Scheduled stop point $scheduledStopPointId not found" }

                return@map ScheduledStop(
                    stopId = ScheduledStopId(savedJourney.relationalId, stopPointInJourneyPattern.order.toInt()),
                    stopOnRequest = stopPointInJourneyPattern.isRequestStop ?: false,
                    name = scheduledStopPoint.name.value,
                    journey = savedJourney,
                    arrival = timetabledPassingTime.arrivalTime,
                    departure = timetabledPassingTime.departureTime,
                )
            }.sortedBy { it.stopId.stopOrder }
    }
    
    private fun linkOperatingPeriod(
        journey: ServiceJourney,
        registry: NetexFileRegistry,
        operatingPeriods: Map<String, OperatingPeriod>,
    ): OperatingPeriod {
        val dayTypeRef = journey.dayTypes.dayTypeRef.firstOrNull()
        checkNotNull(dayTypeRef) { "Journey ${journey.id} has no day type" }

        val dayTypeId = dayTypeRef.value.ref
        val dayTypeAssignment = registry.dayTypeAssignmentRegistryByDayTypeId[dayTypeId]
        checkNotNull(dayTypeAssignment) { "Day type assignment for $dayTypeId not found" }

        val operatingPeriodId = dayTypeAssignment.operatingPeriodRef.value.ref
        val operatingPeriod = operatingPeriods[operatingPeriodId]
        checkNotNull(operatingPeriod) { "Operating period $operatingPeriodId not found" }

        return operatingPeriod
    }
}