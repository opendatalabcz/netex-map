package cz.cvut.fit.gaierda1.data.netex.domain

import cz.cvut.fit.gaierda1.data.netex.NetexFileRegistry
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import org.rutebanken.netex.model.DayTypeAssignment
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.springframework.stereotype.Component

@Component
class JourneyDomainAssembler {
    fun assembleJourneys(
        registry: NetexFileRegistry,
        lineVersions: Map<LineId, LineVersion>,
        operatingPeriods: Map<String, OperatingPeriod>,
    ): Map<JourneyId, Journey> {
        val journeys = mutableMapOf<JourneyId, Journey>()
        for (journey in registry.serviceJourneyRegistry.values) {
            val journeyPatternId = JourneyPatternId(journey.journeyPatternRef.value.ref)
            val patternRegistryValue = registry.serviceJourneyPatternRegistry[journeyPatternId.value]
            checkNotNull(patternRegistryValue) { "Journey pattern ${journeyPatternId.value} not found" }

            val lineId = LineId(patternRegistryValue.serviceJourneyPattern.routeView.lineRef.value.ref)
            val lineVersion = lineVersions[lineId]
            checkNotNull(lineVersion) { "Line ${lineId.value} not found" }

            val journeyId = JourneyId(journey.id)
            journeys[journeyId] = Journey(
                journeyId = journeyId,
                lineVersion = lineVersion,
                journeyPatternId = journeyPatternId,
                schedule = assembleScheduledStops(
                    journey,
                    patternRegistryValue.stopPointInJourneyPatternRegistry,
                    registry.scheduledStopPointRegistry,
                ),
                operatingPeriods = linkOperatingPeriods(
                    journey,
                    registry.dayTypeAssignmentRegistryByDayTypeId,
                    operatingPeriods
                ),
                route = null,
            )
        }
        return journeys
    }

    private fun assembleScheduledStops(
        journey: ServiceJourney,
        stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
        scheduledStopPointRegistry: Map<String, ScheduledStopPoint>,
    ): List<ScheduledStop> {
        return journey.passingTimes.timetabledPassingTime.map { timetabledPassingTime ->
            val stopPointInJourneyPatternId = timetabledPassingTime.pointInJourneyPatternRef.value.ref
            val stopPointInJourneyPattern = stopPointInJourneyPatternRegistry[stopPointInJourneyPatternId]
            checkNotNull(stopPointInJourneyPattern) { "Stop point in journey pattern $stopPointInJourneyPatternId not found" }

            val scheduledStopPointId = stopPointInJourneyPattern.scheduledStopPointRef.value.ref
            val scheduledStopPoint = scheduledStopPointRegistry[scheduledStopPointId]
            checkNotNull(scheduledStopPoint) { "Scheduled stop point $scheduledStopPointId not found" }

            return@map ScheduledStop(
                name = scheduledStopPoint.name.value,
                stopOnRequest = stopPointInJourneyPattern.isRequestStop ?: false,
                arrival = timetabledPassingTime.arrivalTime,
                departure = timetabledPassingTime.departureTime,
            )
        }
    }

    private fun linkOperatingPeriods(
        journey: ServiceJourney,
        dayTypeAssignmentRegistryByDayTypeId: Map<String, DayTypeAssignment>,
        operatingPeriods: Map<String, OperatingPeriod>,
    ): List<OperatingPeriod> {
        return journey.dayTypes.dayTypeRef.map { dayTypeRef ->
            val dayTypeId = dayTypeRef.value.ref
            val dayTypeAssignment = dayTypeAssignmentRegistryByDayTypeId[dayTypeId]
            checkNotNull(dayTypeAssignment) { "Day type assignment for $dayTypeId not found" }

            val operatingPeriodId = dayTypeAssignment.operatingPeriodRef.value.ref
            val operatingPeriod = operatingPeriods[operatingPeriodId]
            checkNotNull(operatingPeriod) { "Operating period $operatingPeriodId not found" }

            return@map operatingPeriod
        }
    }
}