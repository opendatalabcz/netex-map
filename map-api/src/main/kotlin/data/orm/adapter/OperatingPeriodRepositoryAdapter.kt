package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.repository.OperatingPeriodRepository
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
        val optionalSaved = operatingPeriodJpaRepository.findByLineVersionIdAndValidDays(
            fromDate = operatingPeriod.fromDate,
            toDate = operatingPeriod.toDate,
            timezone = operatingPeriod.timezone,
            validDays = operatingPeriod.validDays,
        )
        return optionalSaved.orElseGet { toDb(operatingPeriod, null) }
    }

    fun saveDb(operatingPeriod: DbOperatingPeriod) {
        operatingPeriodJpaRepository.save(operatingPeriod)
    }

    fun saveAllDb(operatingPeriods: Iterable<DbOperatingPeriod>) {
        operatingPeriodJpaRepository.saveAll(operatingPeriods)
    }

    fun findSaveMapping(operatingPeriod: OperatingPeriod): DbOperatingPeriod {
        val mapped = findOrMap(operatingPeriod)
        if (mapped.relationalId == null) saveDb(mapped)
        return mapped
    }

    private val operatingPeriodComparator = compareBy<OperatingPeriod> { it.fromDate }
        .thenBy { it.toDate }
        .thenBy { it.timezone.id }
        .thenComparator { a, b -> a.validDays.compareTo(b.validDays) }

    private fun findSaveMappingsImpl(operatingPeriods: Iterable<OperatingPeriod>, result: Boolean): List<DbOperatingPeriod>? {
        val uniqueOperatingPeriods = sortedSetOf(comparator = operatingPeriodComparator)
        uniqueOperatingPeriods.addAll(operatingPeriods)
        val mappedUniqueOperatingPeriods = uniqueOperatingPeriods.map(::findOrMap)
        saveAllDb(mappedUniqueOperatingPeriods.filter { it.relationalId == null })
        return if (result) operatingPeriods.map { domainOperatingPeriod -> mappedUniqueOperatingPeriods.find { dbOperatingPeriod ->
                    domainOperatingPeriod.fromDate.equals(dbOperatingPeriod.fromDate)
                        && domainOperatingPeriod.toDate.equals(dbOperatingPeriod.toDate)
                        && domainOperatingPeriod.timezone.id == dbOperatingPeriod.timezone.id
                        && domainOperatingPeriod.validDays == dbOperatingPeriod.validDays
                }!! }
            else null
    }

    fun findSaveMappings(operatingPeriods: Iterable<OperatingPeriod>): List<DbOperatingPeriod> {
        return findSaveMappingsImpl(operatingPeriods, result = true)!!
    }

    override fun saveAllIfAbsent(operatingPeriods: Iterable<OperatingPeriod>) {
        findSaveMappingsImpl(operatingPeriods, false)
    }
}