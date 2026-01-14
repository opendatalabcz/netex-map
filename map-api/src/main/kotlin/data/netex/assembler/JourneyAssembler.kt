package cz.cvut.fit.gaierda1.data.netex.assembler

import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStopId
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.springframework.stereotype.Component

@Component
class JourneyAssembler {
    fun assembleJourneys(
        registry: NetexFileRegistry,
        lineVersions: Map<LineId, LineVersion>,
        timetableStops: Map<TimetableStopId, TimetableStop>,
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
                schedule = assembleScheduledStops(journey, patternRegistryValue.stopPointInJourneyPatternRegistry, timetableStops),
                operatingPeriods = linkOperatingPeriods(journey, registry, operatingPeriods),
                route = null,
            )
        }
        return journeys
    }

    private fun assembleScheduledStops(
        journey: ServiceJourney,
        stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
        timetableStops: Map<TimetableStopId, TimetableStop>,
    ): List<ScheduledStop> {
        return journey.passingTimes.timetabledPassingTime.map { timetabledPassingTime ->
            val stopPointInJourneyPatternId = timetabledPassingTime.pointInJourneyPatternRef.value.ref
            val stopPointInJourneyPattern = stopPointInJourneyPatternRegistry[stopPointInJourneyPatternId]
            checkNotNull(stopPointInJourneyPattern) { "Stop point in journey pattern $stopPointInJourneyPatternId not found" }

            val timetableStopId = TimetableStopId(stopPointInJourneyPattern.scheduledStopPointRef.value.ref)
            val timetableStop = timetableStops[timetableStopId]
            checkNotNull(timetableStop) { "Timetable stop $timetableStopId not found" }

            return@map ScheduledStop(
                timetableStop = timetableStop,
                arrival = timetabledPassingTime.arrivalTime,
                departure = timetabledPassingTime.departureTime,
            )
        }
    }
    
    private fun linkOperatingPeriods(
        journey: ServiceJourney,
        registry: NetexFileRegistry,
        operatingPeriods: Map<String, OperatingPeriod>,
    ): List<OperatingPeriod> {
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
