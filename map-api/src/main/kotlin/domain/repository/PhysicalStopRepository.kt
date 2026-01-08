package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId

interface PhysicalStopRepository {
    fun save(physicalStop: PhysicalStop)
    fun findById(id: PhysicalStopId): PhysicalStop?
}
