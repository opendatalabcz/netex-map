package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbTimetableStop
import cz.cvut.fit.gaierda1.data.orm.repository.TimetableStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStopId
import cz.cvut.fit.gaierda1.domain.repository.TimetableStopRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TimetableStopRepositoryAdapter(
    private val timetableStopJpaRepository: TimetableStopJpaRepository,
): TimetableStopRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toDomain(timetableStop: DbTimetableStop): TimetableStop = TimetableStop(
        stopId = TimetableStopId(timetableStop.externalId),
        name = timetableStop.name
    )

    fun toDb(timetableStop: TimetableStop, relationalId: Long?): DbTimetableStop = DbTimetableStop(
        relationalId = relationalId,
        externalId = timetableStop.stopId.value,
        name = timetableStop.name
    )

    fun findSaveMapping(timetableStop: TimetableStop): DbTimetableStop {
        val optionalSaved = timetableStopJpaRepository.findByExternalId(timetableStop.stopId.value)
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            if (timetableStop.name == saved.name) return saved
            else logDifference("name", saved.name, timetableStop.name, saved)
        }
        val mapped = toDb(timetableStop, optionalSaved.map { it.relationalId }.orElse(null))
        return timetableStopJpaRepository.save(mapped)
    }

    override fun save(timetableStop: TimetableStop) {
        findSaveMapping(timetableStop)
    }

    override fun findById(id: TimetableStopId): TimetableStop? {
        return timetableStopJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbTimetableStop) {
        log.warn(
            "Timetable stop {}: {} changed from {} to {}",
            context.externalId,
            fieldName,
            old,
            new
        )
    }
}
