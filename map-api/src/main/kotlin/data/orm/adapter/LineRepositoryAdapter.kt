package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.LineMapper
import cz.cvut.fit.gaierda1.data.orm.repository.LineJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.repository.LineRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LineRepositoryAdapter(
    private val lineJpaRepository: LineJpaRepository,
    private val lineMapper: LineMapper,
): LineRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun save(line: Line) {
        val mapped = lineMapper.toDb(line)
        val optionalSaved = lineJpaRepository.findByExternalId(line.lineId.value)
        optionalSaved.ifPresent { saved ->
            mapped.relationalId = saved.relationalId
            checkAndLogDifference("public code", saved.publicCode, mapped.publicCode, line)
        }
        lineJpaRepository.save(mapped)
    }

    override fun findById(id: LineId): Line? {
        return lineJpaRepository
            .findByExternalId(id.value)
            .map(lineMapper::toDomain)
            .orElse(null)
    }

    private fun checkAndLogDifference(fieldName: String, old: Any?, new: Any?, context: Line) {
        if (old != new) {
            log.warn(
                "Line {}: {} changed from {} to {}",
                context.lineId.value,
                fieldName,
                old,
                new
            )
        }
    }
}
