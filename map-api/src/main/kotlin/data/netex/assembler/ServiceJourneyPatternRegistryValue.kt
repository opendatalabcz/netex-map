package cz.cvut.fit.gaierda1.data.netex.assembler

import org.rutebanken.netex.model.ServiceJourneyPattern
import org.rutebanken.netex.model.StopPointInJourneyPattern

data class ServiceJourneyPatternRegistryValue(
    val serviceJourneyPattern: ServiceJourneyPattern,
    val stopPointInJourneyPatternRegistry: Map<String, StopPointInJourneyPattern>,
) {
}
