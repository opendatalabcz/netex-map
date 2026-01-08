package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.LineMapper
import cz.cvut.fit.gaierda1.data.orm.repository.LineJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.repository.LineRepository
import org.springframework.stereotype.Component

@Component
class LineRepositoryAdapter(
    private val lineJpaRepository: LineJpaRepository,
    private val lineMapper: LineMapper,
): LineRepository {
    override fun save(line: Line) {
        lineJpaRepository.save(lineMapper.toDb(line))
    }

    override fun findById(id: LineId): Line? {
        return lineJpaRepository.findByExternalId(id.value)
            .map(lineMapper::toDomain)
            .orElse(null)
    }
}
