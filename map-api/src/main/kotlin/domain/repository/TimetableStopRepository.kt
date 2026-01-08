package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStopId

interface TimetableStopRepository {
    fun save(timetableStop: TimetableStop)
    fun findById(id: TimetableStopId): TimetableStop?
}
