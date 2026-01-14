package cz.cvut.fit.gaierda1.data.netex.assembler

import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStopId
import org.springframework.stereotype.Component

@Component
class TimetableStopAssembler {
    fun assembleTimetableStops(registry: NetexFileRegistry): Map<TimetableStopId, TimetableStop> {
        val stops = mutableMapOf<TimetableStopId, TimetableStop>()
        for (stop in registry.scheduledStopPointRegistry.values) {
            val stopId = TimetableStopId(stop.id)
            stops[stopId] = TimetableStop(stopId, stop.name.value)
        }
        return stops
    }
}
