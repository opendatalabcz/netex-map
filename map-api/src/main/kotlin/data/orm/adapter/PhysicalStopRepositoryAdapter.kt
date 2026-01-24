package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
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
    private val geometryAdapter: GeometryAdapter,
): PhysicalStopRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toDomain(physicalStop: DbPhysicalStop): PhysicalStop = PhysicalStop(
        stopId = PhysicalStopId(physicalStop.externalId),
        name = physicalStop.name,
        position = geometryAdapter.toDomain(physicalStop.position),
        tags = physicalStop.tags
    )

    fun toDb(physicalStop: PhysicalStop, relationalId: Long?): DbPhysicalStop = DbPhysicalStop(
        relationalId = relationalId,
        externalId = physicalStop.stopId.value,
        name = physicalStop.name,
        position = geometryAdapter.toDb(physicalStop.position),
        tags = physicalStop.tags
    )

    fun findSaveMapping(physicalStop: PhysicalStop): DbPhysicalStop {
        val optionalSaved = physicalStopJpaRepository.findByExternalId(physicalStop.stopId.value)
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            var differ = false
            if (physicalStop.name != saved.name) {
                differ = true
                logDifference("name", saved.name, physicalStop.name, saved)
            }
            if (physicalStop.position != saved.position) {
                differ = true
                logDifference("position", saved.position, physicalStop.position, saved)
            }
            for (key in physicalStop.tags.keys + saved.tags.keys) {
                val newVal = physicalStop.tags[key]
                val oldVal = saved.tags[key]
                if (newVal != oldVal) {
                    differ = true
                    logDifference("tag: $key", oldVal, newVal, saved)
                }
            }
            if (!differ) return saved
        }
        val mapped = toDb(physicalStop, optionalSaved.map { it.relationalId }.orElse(null))
        return physicalStopJpaRepository.save(mapped)
    }
    
    override fun save(physicalStop: PhysicalStop) {
        findSaveMapping(physicalStop)
    }

    override fun findById(id: PhysicalStopId): PhysicalStop? {
        return physicalStopJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }
    
    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbPhysicalStop) {
        log.warn(
            "Physical stop {}: {} changed from {} to {}",
            context.externalId,
            fieldName,
            old,
            new
        )
    }
}
