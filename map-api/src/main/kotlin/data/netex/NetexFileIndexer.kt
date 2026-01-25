package cz.cvut.fit.gaierda1.data.netex

import org.rutebanken.netex.model.CompositeFrame
import org.rutebanken.netex.model.DayTypeAssignment
import org.rutebanken.netex.model.Line
import org.rutebanken.netex.model.PublicationDeliveryStructure
import org.rutebanken.netex.model.ResourceFrame
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceCalendarFrame
import org.rutebanken.netex.model.ServiceFrame
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.ServiceJourneyPattern
import org.rutebanken.netex.model.SiteFrame
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.rutebanken.netex.model.TimetableFrame
import org.rutebanken.netex.model.UicOperatingPeriod
import org.springframework.stereotype.Component

@Component
class NetexFileIndexer {
    fun createRegistry(publicationDelivery: PublicationDeliveryStructure): NetexFileRegistry {
        val rootFrames = publicationDelivery.dataObjects.compositeFrameOrCommonFrame
        if (rootFrames.size != 1) error("Expected exactly one root frame, found ${rootFrames.size}")
        val rootFrame = rootFrames.first()
        val compositeFrame = rootFrame.value as? CompositeFrame ?: error("Unexpected frame type: ${rootFrame.value::class}")
        val childFrames = compositeFrame.frames.commonFrame.map { it.value }

        val serviceFrame = childFrames.filterIsInstance<ServiceFrame>().singleOrThrow("service frame")
        val siteFrame = childFrames.filterIsInstance<SiteFrame>().singleOrThrow("site frame")
        val timetableFrame = childFrames.filterIsInstance<TimetableFrame>().singleOrThrow("timetable frame")
        val serviceCalendarFrame = childFrames.filterIsInstance<ServiceCalendarFrame>().singleOrThrow("service calendar frame")
        val resourceFrame = childFrames.filterIsInstance<ResourceFrame>().singleOrThrow("resource frame")

        return NetexFileRegistry(
            frameDefaults = compositeFrame.frameDefaults,
            scheduledStopPointRegistry = indexScheduledStopPoints(serviceFrame),
            serviceJourneyPatternRegistry = indexServiceJourneyPatterns(serviceFrame),
            serviceJourneyRegistry = indexServiceJourneys(timetableFrame),
            uicOperatingPeriodRegistry = indexUicOperatingPeriods(serviceCalendarFrame),
            dayTypeAssignmentRegistryByDayTypeId = indexDayTypeAssignments(serviceCalendarFrame),
            lineRegistry = indexLines(serviceFrame),
        )
    }

    private fun indexScheduledStopPoints(serviceFrame: ServiceFrame): Map<String, ScheduledStopPoint> {
        val scheduledStopPointRegistry = mutableMapOf<String, ScheduledStopPoint>()
        for (scheduledStopPoint in serviceFrame.scheduledStopPoints.scheduledStopPoint) {
            scheduledStopPointRegistry[scheduledStopPoint.id] = scheduledStopPoint
        }
        return scheduledStopPointRegistry
    }

    private fun indexServiceJourneyPatterns(serviceFrame: ServiceFrame): Map<String, ServiceJourneyPatternRegistryValue> {
        val serviceJourneyPatternRegistry = mutableMapOf<String, ServiceJourneyPatternRegistryValue>()
        for (journeyPattern in serviceFrame.journeyPatterns.journeyPattern_OrJourneyPatternView) {
            val serviceJourneyPattern = journeyPattern.value as? ServiceJourneyPattern ?: error("Unexpected journey pattern type: ${journeyPattern.value::class}")
            val stopPointInJourneyPatternRegistry = mutableMapOf<String, StopPointInJourneyPattern>()
            for (pointInSequence in serviceJourneyPattern.pointsInSequence.pointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern) {
                val stopPointInJourneyPattern = pointInSequence as? StopPointInJourneyPattern ?: error("Unexpected point in sequence type: ${pointInSequence::class}")
                stopPointInJourneyPatternRegistry[stopPointInJourneyPattern.id] = stopPointInJourneyPattern
            }
            serviceJourneyPatternRegistry[serviceJourneyPattern.id] =
                ServiceJourneyPatternRegistryValue(serviceJourneyPattern, stopPointInJourneyPatternRegistry)
        }
        return serviceJourneyPatternRegistry
    }

    private fun indexServiceJourneys(timetableFrame: TimetableFrame): Map<String, ServiceJourney> {
        val serviceJourneyRegistry = mutableMapOf<String, ServiceJourney>()
        for (journey in timetableFrame.vehicleJourneys.vehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney) {
            val serviceJourney = journey as? ServiceJourney ?: error("Unexpected vehicle journey type: ${journey::class}")
            serviceJourneyRegistry[serviceJourney.id] = serviceJourney
        }
        return serviceJourneyRegistry
    }

    private fun indexUicOperatingPeriods(serviceCalendarFrame: ServiceCalendarFrame): Map<String, UicOperatingPeriod> {
        val uicOperatingPeriodRegistry = mutableMapOf<String, UicOperatingPeriod>()
        for (operatingPeriod in serviceCalendarFrame.serviceCalendar.operatingPeriods.operatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod) {
            val uicOperatingPeriod = operatingPeriod.value as? UicOperatingPeriod ?: error("Unexpected operating period type: ${operatingPeriod::class}")
            uicOperatingPeriodRegistry[uicOperatingPeriod.id] = uicOperatingPeriod
        }
        return uicOperatingPeriodRegistry
    }

    private fun indexDayTypeAssignments(serviceCalendarFrame: ServiceCalendarFrame): Map<String, DayTypeAssignment> {
        val dayTypeAssignmentRegistryByDayTypeId = mutableMapOf<String, DayTypeAssignment>()
        for (dayTypeAssignment in serviceCalendarFrame.serviceCalendar.dayTypeAssignments.dayTypeAssignment) {
            dayTypeAssignmentRegistryByDayTypeId[dayTypeAssignment.dayTypeRef.value.ref] = dayTypeAssignment
        }
        return dayTypeAssignmentRegistryByDayTypeId
    }

    private fun indexLines(serviceFrame: ServiceFrame): Map<String, Line> {
        val lineRegistry = mutableMapOf<String, Line>()
        for (lineElement in serviceFrame.lines.line_) {
            val line = lineElement.value as? Line ?: error("Unexpected line type: ${lineElement.value::class}")
            lineRegistry[line.id] = line
        }
        return lineRegistry
    }

    private fun <T> Collection<T>.singleOrThrow(elementName: String): T {
        return if (size == 1) first() else error("Expected exactly one $elementName, found $size")
    }
}