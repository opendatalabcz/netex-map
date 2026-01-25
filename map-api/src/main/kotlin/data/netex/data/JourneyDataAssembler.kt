package cz.cvut.fit.gaierda1.data.netex.data

import cz.cvut.fit.gaierda1.data.netex.NetexFileRegistry
import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.springframework.stereotype.Component

@Component
class JourneyDataAssembler(
    private val journeyJpaRepository: JourneyJpaRepository,
) {
    fun assembleJourneys(
        registry: NetexFileRegistry,
        lineVersions: Map<String, DbLineVersion>,
        operatingPeriods: Map<String, DbOperatingPeriod>,
    ): Map<String, DbJourney> {
        val journeys = mutableMapOf<String, DbJourney>()
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
                    timezone = lineVersion.timezone,
                    isDetour = lineVersion.isDetour,
                ).orElseGet { assembleJourney(
                    journey = journey,
                    journeyPatternId = journeyPatternId,
                    lineVersion = lineVersion,
                    registry = registry,
                    stopPointInJourneyPatternRegistry = patternRegistryValue.stopPointInJourneyPatternRegistry,
                    operatingPeriods = operatingPeriods
                ) }
        }
        return journeys
    }

    private fun assembleJourney(
        journey: ServiceJourney,
        journeyPatternId: String,
        lineVersion: DbLineVersion,
        registry: NetexFileRegistry,
        stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
        operatingPeriods: Map<String, DbOperatingPeriod>,
    ): DbJourney {
        val schedule = mutableListOf<DbScheduledStop>()
        val savedJourney = DbJourney(
            relationalId = null,
            externalId = journey.id,
            lineVersion = lineVersion,
            journeyPatternId = journeyPatternId,
            schedule = schedule,
            operatingPeriods = linkOperatingPeriods(journey, registry, operatingPeriods),
            route = null,
        )
        schedule.addAll(assembleScheduledStops(
            serviceJourney = journey,
            savedJourney = savedJourney,
            stopPointInJourneyPatternRegistry = stopPointInJourneyPatternRegistry,
            scheduledStopPointRegistry = registry.scheduledStopPointRegistry
        ))
        return savedJourney
    }
    
    private fun assembleScheduledStops(
        serviceJourney: ServiceJourney,
        savedJourney: DbJourney,
        stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
        scheduledStopPointRegistry: Map<String, ScheduledStopPoint>,
    ): List<DbScheduledStop> {
        return serviceJourney.passingTimes.timetabledPassingTime.mapIndexed { index, timetabledPassingTime ->
            val stopPointInJourneyPatternId = timetabledPassingTime.pointInJourneyPatternRef.value.ref
            val stopPointInJourneyPattern = stopPointInJourneyPatternRegistry[stopPointInJourneyPatternId]
            checkNotNull(stopPointInJourneyPattern) { "Stop point in journey pattern $stopPointInJourneyPatternId not found" }

            val scheduledStopPointId = stopPointInJourneyPattern.scheduledStopPointRef.value.ref
            val scheduledStopPoint = scheduledStopPointRegistry[scheduledStopPointId]
            checkNotNull(scheduledStopPoint) { "Scheduled stop point $scheduledStopPointId not found" }

            return@mapIndexed DbScheduledStop(
                name = scheduledStopPoint.name.value,
                arrival = timetabledPassingTime.arrivalTime,
                departure = timetabledPassingTime.departureTime,
                stopId = DbScheduledStopId(savedJourney.relationalId, index),
                journey = savedJourney,
            )
        }
    }
    
    private fun linkOperatingPeriods(
        journey: ServiceJourney,
        registry: NetexFileRegistry,
        operatingPeriods: Map<String, DbOperatingPeriod>,
    ): List<DbOperatingPeriod> {
        return journey.dayTypes.dayTypeRef.map { dayTypeRef ->
            val dayTypeId = dayTypeRef.value.ref
            val dayTypeAssignment = registry.dayTypeAssignmentRegistryByDayTypeId[dayTypeId]
            checkNotNull(dayTypeAssignment) { "Day type assignment for $dayTypeId not found" }

            val operatingPeriodId = dayTypeAssignment.operatingPeriodRef.value.ref
            val operatingPeriod = operatingPeriods[operatingPeriodId]
            checkNotNull(operatingPeriod) { "Operating period $operatingPeriodId not found" }

            return@map operatingPeriod
        }
    }
}