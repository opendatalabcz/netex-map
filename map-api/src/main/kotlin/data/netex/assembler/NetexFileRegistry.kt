package cz.cvut.fit.gaierda1.data.netex.assembler

import org.rutebanken.netex.model.DayTypeAssignment
import org.rutebanken.netex.model.Line
import org.rutebanken.netex.model.ScheduledStopPoint
import org.rutebanken.netex.model.ServiceJourney
import org.rutebanken.netex.model.UicOperatingPeriod
import org.rutebanken.netex.model.VersionFrameDefaultsStructure


data class NetexFileRegistry (
    val frameDefaults: VersionFrameDefaultsStructure,
    val scheduledStopPointRegistry: Map<String, ScheduledStopPoint>,
    val serviceJourneyPatternRegistry: Map<String, ServiceJourneyPatternRegistryValue>,
    val serviceJourneyRegistry: Map<String, ServiceJourney>,
    val uicOperatingPeriodRegistry: Map<String, UicOperatingPeriod>,
    val dayTypeAssignmentRegistryByDayTypeId: Map<String, DayTypeAssignment>,
    val lineRegistry: Map<String, Line>,
) {
}
