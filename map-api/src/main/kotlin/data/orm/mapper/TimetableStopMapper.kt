package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbTimetableStop
import cz.cvut.fit.gaierda1.data.orm.repository.TimetableStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStopId
import org.springframework.stereotype.Component

@Component
class TimetableStopMapper(
    private val timetableStopJpaRepository: TimetableStopJpaRepository,
) {
    fun toDomain(timetableStop: DbTimetableStop): TimetableStop = TimetableStop(
        stopId = TimetableStopId(timetableStop.externalId),
        name = timetableStop.name
    )

    fun toDb(timetableStop: TimetableStop): DbTimetableStop {
        val saved = timetableStopJpaRepository.findByExternalId(timetableStop.stopId.value)
        return DbTimetableStop(
            relationalId = saved.map { it.relationalId }.orElse(null),
            externalId = timetableStop.stopId.value,
            name = timetableStop.name
        )
    }
}
