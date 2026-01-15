package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.TimetableStopMapper
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
    private val timetableStopMapper: TimetableStopMapper,
): TimetableStopRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun save(timetableStop: TimetableStop) {
        val mapped = timetableStopMapper.toDb(timetableStop)
        val optionalSaved = timetableStopJpaRepository.findByExternalId(timetableStop.stopId.value)
        optionalSaved.ifPresent { saved ->
            mapped.relationalId = saved.relationalId
            checkAndLogDifference("name", saved.name, mapped.name, timetableStop)
        }
        timetableStopJpaRepository.save(mapped)
    }

    override fun findById(id: TimetableStopId): TimetableStop? {
        return timetableStopJpaRepository
            .findByExternalId(id.value)
            .map(timetableStopMapper::toDomain)
            .orElse(null)
    }
    
    private fun checkAndLogDifference(fieldName: String, old: Any?, new: Any?, context: TimetableStop) {
        if (old != new) {
            log.warn(
                "Timetable stop {}: {} changed from {} to {}",
                context.stopId.value,
                fieldName,
                old,
                new
            )
        }
    }
}
