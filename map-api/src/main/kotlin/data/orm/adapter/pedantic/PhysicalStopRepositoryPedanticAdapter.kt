package cz.cvut.fit.gaierda1.data.orm.adapter.pedantic

import cz.cvut.fit.gaierda1.data.orm.adapter.GeometryAdapter
import cz.cvut.fit.gaierda1.data.orm.adapter.PhysicalStopRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PhysicalStopRepositoryPedanticAdapter(
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    geometryAdapter: GeometryAdapter,
): PhysicalStopRepositoryAdapter(physicalStopJpaRepository, geometryAdapter) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun findOrMap(physicalStop: PhysicalStop): DbPhysicalStop {
        val optionalSaved = physicalStopJpaRepository.findByExternalId(physicalStop.stopId.value)
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            if (physicalStop.name != saved.name) {
                logDifference("name", saved.name, physicalStop.name, saved)
            }
            if (physicalStop.position != saved.position) {
                logDifference("position", saved.position, physicalStop.position, saved)
            }
            for (key in physicalStop.tags.keys + saved.tags.keys) {
                val newVal = physicalStop.tags[key]
                val oldVal = saved.tags[key]
                if (newVal != oldVal) {
                    logDifference("tag: $key", oldVal, newVal, saved)
                }
            }
            return saved
        }
        return toDb(physicalStop, null)
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
