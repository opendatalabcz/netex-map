package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.TimetableStopMapper
import cz.cvut.fit.gaierda1.data.orm.repository.TimetableStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import cz.cvut.fit.gaierda1.domain.model.TimetableStopId
import cz.cvut.fit.gaierda1.domain.repository.TimetableStopRepository
import org.springframework.stereotype.Component

@Component
class TimetableStopRepositoryAdapter(
    private val timetableStopJpaRepository: TimetableStopJpaRepository,
    private val timetableStopMapper: TimetableStopMapper,
): TimetableStopRepository {
    override fun save(timetableStop: TimetableStop) {
        timetableStopJpaRepository.save(timetableStopMapper.toDb(timetableStop))
    }

    override fun findById(id: TimetableStopId): TimetableStop? {
        return timetableStopJpaRepository.findByExternalId(id.value)
            .map(timetableStopMapper::toDomain)
            .orElse(null)
    }
}
