package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import cz.cvut.fit.gaierda1.domain.repository.PhysicalStopRepository
import org.springframework.stereotype.Component

@Component
open class PhysicalStopRepositoryAdapter(
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val geometryAdapter: GeometryAdapter,
): PhysicalStopRepository {
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

    fun findOrMap(physicalStop: PhysicalStop): DbPhysicalStop {
        val optionalSaved = physicalStopJpaRepository.findByExternalId(physicalStop.stopId.value)
        return optionalSaved.orElseGet { toDb(physicalStop, null) }
    }

    fun saveDb(physicalStop: DbPhysicalStop) {
        physicalStopJpaRepository.save(physicalStop)
    }

    fun saveAllDb(physicalStops: Iterable<DbPhysicalStop>) {
        physicalStopJpaRepository.saveAll(physicalStops)
    }

    fun findSaveMapping(physicalStop: PhysicalStop): DbPhysicalStop {
        val mapped = findOrMap(physicalStop)
        if (mapped.relationalId == null) saveDb(mapped)
        return mapped
    }

    private val physicalStopComparator = compareBy<PhysicalStop> { it.stopId.value }

    private fun findSaveMappingsImpl(physicalStops: Iterable<PhysicalStop>, result: Boolean): List<DbPhysicalStop>? {
        val uniqueStops = sortedSetOf(comparator = physicalStopComparator)
        uniqueStops.addAll(physicalStops)
        val mappedUniqueStops = uniqueStops.map(::findOrMap)
        saveAllDb(mappedUniqueStops.filter { it.relationalId == null })
        return if (result) physicalStops.map { domainStop -> mappedUniqueStops.find { dbStop -> domainStop.stopId.value == dbStop.externalId }!! }
            else null
    }

    fun findSaveMappings(physicalStops: Iterable<PhysicalStop>): List<DbPhysicalStop> {
        return findSaveMappingsImpl(physicalStops, true)!!
    }
    
    override fun saveIfAbsent(physicalStop: PhysicalStop) {
        findSaveMapping(physicalStop)
    }

    override fun saveAllIfAbsent(physicalStops: Iterable<PhysicalStop>) {
        findSaveMappingsImpl(physicalStops, false)
    }

    override fun findById(id: PhysicalStopId): PhysicalStop? {
        return physicalStopJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }
}
