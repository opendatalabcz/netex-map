package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId

interface PhysicalStopRepository {
    fun saveIfAbsent(physicalStop: PhysicalStop)
    fun saveAllIfAbsent(physicalStops: Iterable<PhysicalStop>)
    fun findById(id: PhysicalStopId): PhysicalStop?
}
