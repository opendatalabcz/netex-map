package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.PhysicalStopMapper
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import cz.cvut.fit.gaierda1.domain.repository.PhysicalStopRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PhysicalStopRepositoryAdapter(
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val physicalStopMapper: PhysicalStopMapper,
): PhysicalStopRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun save(physicalStop: PhysicalStop) {
        val mapped = physicalStopMapper.toDb(physicalStop)
        val optionalSaved = physicalStopJpaRepository.findByExternalId(physicalStop.stopId.value)
        optionalSaved.ifPresent { saved ->
            mapped.relationalId = saved.relationalId
            checkAndLogDifference("name", saved.name, mapped.name, physicalStop)
            for (key in saved.tags.keys + mapped.tags.keys) {
                checkAndLogDifference("tag: $key", saved.tags[key], mapped.tags[key], physicalStop)
            }
        }
        physicalStopJpaRepository.save(mapped)
    }

    override fun findById(id: PhysicalStopId): PhysicalStop? {
        return physicalStopJpaRepository
            .findByExternalId(id.value)
            .map(physicalStopMapper::toDomain)
            .orElse(null)
    }
    
    private fun checkAndLogDifference(fieldName: String, old: Any?, new: Any?, context: PhysicalStop) {
        if (old != new) {
            log.warn(
                "Physical stop {}: {} changed from {} to {}",
                context.stopId.value,
                fieldName,
                old,
                new
            )
        }
    }
}
