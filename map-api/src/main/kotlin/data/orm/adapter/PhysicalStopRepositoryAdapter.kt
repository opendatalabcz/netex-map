package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import cz.cvut.fit.gaierda1.domain.repository.PhysicalStopRepository
import cz.cvut.fit.gaierda1.measuring.Measurer
import org.springframework.stereotype.Component

@Component
class PhysicalStopRepositoryAdapter(
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
        ++Measurer.searchedPhysicalStops
        val optionalSaved = Measurer.addToDbFind { physicalStopJpaRepository.findByExternalId(physicalStop.stopId.value) }
        return optionalSaved.orElseGet { toDb(physicalStop, null) }
    }

    fun saveDb(physicalStop: DbPhysicalStop) {
        ++Measurer.savedPhysicalStops
        Measurer.addToDbSave { physicalStopJpaRepository.save(physicalStop) }
    }

    fun saveAllDb(physicalStops: Iterable<DbPhysicalStop>) {
        Measurer.savedPhysicalStops += physicalStops.count()
        Measurer.addToDbSave { physicalStopJpaRepository.saveAll(physicalStops) }
    }

    fun findSaveMapping(physicalStop: PhysicalStop): FindSaveSingleMapping {
        val mapped = findOrMap(physicalStop)
        return FindSaveSingleMapping(mapped, mapped.relationalId == null)
    }

    private val physicalStopComparator = compareBy<PhysicalStop> { it.stopId.value }

    private fun findSaveMappingsImpl(physicalStops: Iterable<PhysicalStop>, mappedResult: Boolean): Pair<List<DbPhysicalStop>?, List<DbPhysicalStop>> {
        val uniqueStops = sortedSetOf(comparator = physicalStopComparator)
        uniqueStops.addAll(physicalStops)
        val mappedUniqueStops = uniqueStops.map(::findOrMap)
        return (if (mappedResult) physicalStops.map { domainStop -> mappedUniqueStops.find { dbStop -> domainStop.stopId.value == dbStop.externalId }!! }
            else null) to mappedUniqueStops.filter { it.relationalId == null }
    }

    fun findSaveMappings(physicalStops: Iterable<PhysicalStop>): FindSaveMultipleMapping {
        val res = findSaveMappingsImpl(physicalStops, true)
        return FindSaveMultipleMapping(res.first!!, res.second)
    }
    
    override fun saveIfAbsent(physicalStop: PhysicalStop) {
        val mapping = findSaveMapping(physicalStop)
        if (mapping.save) saveDb(mapping.physicalStop)
    }

    override fun saveAllIfAbsent(physicalStops: Iterable<PhysicalStop>) {
        val toSave = findSaveMappingsImpl(physicalStops, false).second
        if (toSave.isNotEmpty()) saveAllDb(toSave)
    }

    override fun findById(id: PhysicalStopId): PhysicalStop? {
        return physicalStopJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }

    data class FindSaveSingleMapping(val physicalStop: DbPhysicalStop, val save: Boolean)
    data class FindSaveMultipleMapping(val physicalStops: List<DbPhysicalStop>, val toSavePhysicalStops: List<DbPhysicalStop>)
}
