package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.repository.OperatingPeriodRepository
import cz.cvut.fit.gaierda1.measuring.Measurer
import org.springframework.stereotype.Component

@Component
class OperatingPeriodRepositoryAdapter(
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
): OperatingPeriodRepository {
    fun toDomain(operatingPeriod: DbOperatingPeriod): OperatingPeriod = OperatingPeriod(
        timezone = operatingPeriod.timezone,
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays
    )

    fun toDb(operatingPeriod: OperatingPeriod, relationalId: Long?): DbOperatingPeriod = DbOperatingPeriod(
        relationalId = relationalId,
        timezone = operatingPeriod.timezone,
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays,
    )

    fun findOrMap(operatingPeriod: OperatingPeriod): DbOperatingPeriod {
        ++Measurer.searchedOperatingPeriods
        val optionalSaved = Measurer.addToDbFind {
            operatingPeriodJpaRepository.findByLineVersionIdAndValidDays(
                fromDate = operatingPeriod.fromDate,
                toDate = operatingPeriod.toDate,
                timezone = operatingPeriod.timezone,
                validDays = operatingPeriod.validDays,
            )
        }
        return optionalSaved.orElseGet{ toDb(operatingPeriod, null) }
    }

    fun saveDb(operatingPeriod: DbOperatingPeriod) {
        ++Measurer.savedOperatingPeriods
        Measurer.addToDbSave { operatingPeriodJpaRepository.save(operatingPeriod) }
    }

    fun saveAllDb(operatingPeriods: Iterable<DbOperatingPeriod>) {
        Measurer.savedOperatingPeriods += operatingPeriods.count()
        Measurer.addToDbSave { operatingPeriodJpaRepository.saveAll(operatingPeriods) }
    }

    fun findSaveMapping(operatingPeriod: OperatingPeriod): FindSaveSingleMapping {
        val mapped = findOrMap(operatingPeriod)
        return FindSaveSingleMapping(mapped, mapped.relationalId == null)
    }

    private val operatingPeriodComparator = compareBy<OperatingPeriod> { it.fromDate }
        .thenBy { it.toDate }
        .thenBy { it.timezone.id }
        .thenComparator { a, b -> a.validDays.compareTo(b.validDays) }

    private fun findSaveMappingsImpl(operatingPeriods: Iterable<OperatingPeriod>, result: Boolean): Pair<List<DbOperatingPeriod>?, List<DbOperatingPeriod>> {
        val uniqueOperatingPeriods = sortedSetOf(comparator = operatingPeriodComparator)
        uniqueOperatingPeriods.addAll(operatingPeriods)
        val mappedUniqueOperatingPeriods = uniqueOperatingPeriods.map(::findOrMap)
        return (if (result) operatingPeriods.map { domainOperatingPeriod ->
            mappedUniqueOperatingPeriods.find { dbOperatingPeriod ->
                domainOperatingPeriod.fromDate.equals(dbOperatingPeriod.fromDate)
                        && domainOperatingPeriod.toDate.equals(dbOperatingPeriod.toDate)
                        && domainOperatingPeriod.timezone.id == dbOperatingPeriod.timezone.id
                        && domainOperatingPeriod.validDays == dbOperatingPeriod.validDays
            }!!
        } else null) to mappedUniqueOperatingPeriods.filter { it.relationalId == null }
    }

    fun findSaveMappings(operatingPeriods: Iterable<OperatingPeriod>): FindSaveMultipleMapping {
        val res = findSaveMappingsImpl(operatingPeriods, result = true)
        return FindSaveMultipleMapping(res.first!!, res.second)
    }

    override fun saveIfAbsent(operatingPeriod: OperatingPeriod) {
        val mapping = findSaveMapping(operatingPeriod)
        if (mapping.save) saveDb(mapping.operatingPeriod)
    }

    override fun saveAllIfAbsent(operatingPeriods: Iterable<OperatingPeriod>) {
        val toSave = findSaveMappingsImpl(operatingPeriods, false).second
        if (toSave.isNotEmpty()) saveAllDb(toSave)
    }

    data class FindSaveSingleMapping(val operatingPeriod: DbOperatingPeriod, val save: Boolean)
    data class FindSaveMultipleMapping(val operatingPeriods: List<DbOperatingPeriod>, val toSaveOperatingPeriods: List<DbOperatingPeriod>)
}