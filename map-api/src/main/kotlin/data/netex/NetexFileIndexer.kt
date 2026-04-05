package cz.cvut.fit.gaierda1.data.netex

import org.rutebanken.netex.model.CompositeFrame
import org.rutebanken.netex.model.DayTypeAssignment
import org.rutebanken.netex.model.Direction
import org.rutebanken.netex.model.Line
import org.rutebanken.netex.model.Operator
import org.rutebanken.netex.model.PassengerStopAssignment
import org.rutebanken.netex.model.PublicationDeliveryStructure
import org.rutebanken.netex.model.ResourceFrame
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceCalendarFrame
import org.rutebanken.netex.model.ServiceFrame
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.ServiceJourneyPattern
import org.rutebanken.netex.model.ServiceLink
import org.rutebanken.netex.model.SiteFrame
import org.rutebanken.netex.model.StopPlace
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.rutebanken.netex.model.TariffZone
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
            stopPointInJourneyPatternRegistry = indexStopPointInJourneyPatterns(serviceFrame),
            serviceJourneyRegistry = indexServiceJourneys(timetableFrame),
            directionRegistry = indexDirections(serviceFrame),
            uicOperatingPeriodRegistry = indexUicOperatingPeriods(serviceCalendarFrame),
            dayTypeAssignmentRegistryByDayTypeId = indexDayTypeAssignments(serviceCalendarFrame),
            lineRegistry = indexLines(serviceFrame),
            operatorRegistry = indexOperators(resourceFrame),
            stopPlaceRegistry = indexStopPlaces(siteFrame),
            stopAssignmentRegistryByScheduledStopPointId = indexStopAssignments(serviceFrame),
            serviceLinkRegistry = indexServiceLinks(serviceFrame),
            tariffZoneRegistry = indexTariffZones(serviceFrame),
        )
    }

    private fun indexScheduledStopPoints(serviceFrame: ServiceFrame): Map<String, Pair<Int, ScheduledStopPoint>> {
        val scheduledStopPointRegistry = mutableMapOf<String, Pair<Int, ScheduledStopPoint>>()
        for ((idx, scheduledStopPoint) in serviceFrame.scheduledStopPoints.scheduledStopPoint.withIndex()) {
            scheduledStopPointRegistry[scheduledStopPoint.id] = idx to scheduledStopPoint
        }
        return scheduledStopPointRegistry
    }

    private fun indexServiceJourneyPatterns(serviceFrame: ServiceFrame): Map<String, Pair<Int, ServiceJourneyPattern>> {
        val serviceJourneyPatternRegistry = mutableMapOf<String, Pair<Int, ServiceJourneyPattern>>()
        for ((idx, journeyPattern) in serviceFrame.journeyPatterns.journeyPattern_OrJourneyPatternView.withIndex()) {
            val serviceJourneyPattern = journeyPattern.value as? ServiceJourneyPattern ?: error("Unexpected journey pattern type: ${journeyPattern.value::class}")
            val stopPointInJourneyPatternRegistry = mutableMapOf<String, StopPointInJourneyPattern>()
            for (pointInSequence in serviceJourneyPattern.pointsInSequence.pointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern) {
                val stopPointInJourneyPattern = pointInSequence as? StopPointInJourneyPattern ?: error("Unexpected point in sequence type: ${pointInSequence::class}")
                stopPointInJourneyPatternRegistry[stopPointInJourneyPattern.id] = stopPointInJourneyPattern
            }
            serviceJourneyPatternRegistry[serviceJourneyPattern.id] = idx to serviceJourneyPattern
        }
        return serviceJourneyPatternRegistry
    }

    private fun indexStopPointInJourneyPatterns(serviceFrame: ServiceFrame): Map<String, StopPointInJourneyPattern> {
        val stopPointInJourneyPatterns = mutableMapOf<String, StopPointInJourneyPattern>()
        for (journeyPattern in serviceFrame.journeyPatterns.journeyPattern_OrJourneyPatternView) {
            val serviceJourneyPattern = journeyPattern.value as? ServiceJourneyPattern ?: error("Unexpected journey pattern type: ${journeyPattern.value::class}")
            for (pointInSequence in serviceJourneyPattern.pointsInSequence.pointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern) {
                val stopPointInJourneyPattern = pointInSequence as? StopPointInJourneyPattern ?: error("Unexpected point in sequence type: ${pointInSequence::class}")
                stopPointInJourneyPatterns[stopPointInJourneyPattern.id] = stopPointInJourneyPattern
            }
        }
        return stopPointInJourneyPatterns
    }

    private fun indexServiceJourneys(timetableFrame: TimetableFrame): Map<String, ServiceJourney> {
        val serviceJourneyRegistry = mutableMapOf<String, ServiceJourney>()
        for (journey in timetableFrame.vehicleJourneys.vehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney) {
            val serviceJourney = journey as? ServiceJourney ?: error("Unexpected vehicle journey type: ${journey::class}")
            serviceJourneyRegistry[serviceJourney.id] = serviceJourney
        }
        return serviceJourneyRegistry
    }

    private fun indexDirections(serviceFrame: ServiceFrame): Map<String, Direction> {
        val directions = mutableMapOf<String, Direction>()
        for (direction in serviceFrame.directions.direction) {
            directions[direction.id] = direction
        }
        return directions
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

    private fun indexOperators(resourceFrame: ResourceFrame): Map<String, Operator> {
        val operatorRegistry = mutableMapOf<String, Operator>()
        for (operatorElement in resourceFrame.organisations.organisation_) {
            val operator = operatorElement.value as? Operator ?: error("Unexpected organisation type: ${operatorElement.value::class}")
            operatorRegistry[operator.id] = operator
        }
        return operatorRegistry
    }

    private fun indexStopPlaces(siteFrame: SiteFrame): Map<String, StopPlace> {
        val stopPlaceRegistry = mutableMapOf<String, StopPlace>()
        for (stopPlaceElement in siteFrame.stopPlaces.stopPlace_) {
            val stopPlace = stopPlaceElement.value as? StopPlace ?: error("Unexpected stop place type: ${stopPlaceElement.value::class}")
            stopPlaceRegistry[stopPlace.id] = stopPlace
        }
        return stopPlaceRegistry
    }

    private fun indexStopAssignments(serviceFrame: ServiceFrame): Map<String, PassengerStopAssignment> {
        val stopAssignmentRegistryByScheduledStopPointId = mutableMapOf<String, PassengerStopAssignment>()
        for (stopAssignmentElement in serviceFrame.stopAssignments.stopAssignment) {
            val stopAssignment = stopAssignmentElement.value as? PassengerStopAssignment ?: error("Unexpected stop assignment type: ${stopAssignmentElement.value::class}")
            stopAssignmentRegistryByScheduledStopPointId[stopAssignment.scheduledStopPointRef.value.ref] = stopAssignment
        }
        return stopAssignmentRegistryByScheduledStopPointId
    }

    private fun indexServiceLinks(serviceFrame: ServiceFrame): Map<String, ServiceLink> {
        val serviceLinks = mutableMapOf<String, ServiceLink>()
        for (serviceLink in serviceFrame.serviceLinks.serviceLink) {
            serviceLinks[serviceLink.id] = serviceLink
        }
        return serviceLinks
    }

    private fun indexTariffZones(serviceFrame: ServiceFrame): Map<String, TariffZone> {
        if (serviceFrame.tariffZones == null) return emptyMap()
        val tariffZones = mutableMapOf<String, TariffZone>()
        for (tariffZoneElement in serviceFrame.tariffZones.tariffZone) {
            val tariffZone = tariffZoneElement.value as? TariffZone ?: error("Unexpected tariff zone type: ${tariffZoneElement.value::class}")
            tariffZones[tariffZone.id] = tariffZone
        }
        return tariffZones
    }

    private fun <T> Collection<T>.singleOrThrow(elementName: String): T {
        return if (size == 1) first() else error("Expected exactly one $elementName, found $size")
    }
}
