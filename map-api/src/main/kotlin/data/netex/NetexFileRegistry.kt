package cz.cvut.fit.gaierda1.data.netex

import org.rutebanken.netex.model.DayTypeAssignment
import org.rutebanken.netex.model.Direction
import org.rutebanken.netex.model.Line
import org.rutebanken.netex.model.Operator
import org.rutebanken.netex.model.PassengerStopAssignment
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.ServiceJourneyPattern
import org.rutebanken.netex.model.ServiceLink
import org.rutebanken.netex.model.StopPlace
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.rutebanken.netex.model.TariffZone
import org.rutebanken.netex.model.UicOperatingPeriod
import org.rutebanken.netex.model.VersionFrameDefaultsStructure

data class NetexFileRegistry (
    val frameDefaults: VersionFrameDefaultsStructure,
    val scheduledStopPointRegistry: Map<String, Pair<Int, ScheduledStopPoint>>,
    val serviceJourneyPatternRegistry: Map<String, Pair<Int, ServiceJourneyPattern>>,
    val stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
    val serviceJourneyRegistry: Map<String, ServiceJourney>,
    val directionRegistry: Map<String, Direction>,
    val uicOperatingPeriodRegistry: Map<String, UicOperatingPeriod>,
    val dayTypeAssignmentRegistryByDayTypeId: Map<String, DayTypeAssignment>,
    val lineRegistry: Map<String, Line>,
    val operatorRegistry: Map<String, Operator>,
    val stopPlaceRegistry: Map<String, StopPlace>,
    val stopAssignmentRegistryByScheduledStopPointId: Map<String, PassengerStopAssignment>,
    val serviceLinkRegistry: Map<String, ServiceLink>,
    val tariffZoneRegistry: Map<String, TariffZone>,
) {
}
