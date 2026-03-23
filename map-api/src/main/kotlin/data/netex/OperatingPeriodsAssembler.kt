package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import org.springframework.stereotype.Component

@Component
class OperatingPeriodsAssembler(
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
) {
    fun assembleOperatingPeriods(
        registry: NetexFileRegistry,
        operatingPeriodCache: MutableList<OperatingPeriod>,
    ): Map<String, OperatingPeriod> {
        val operatingPeriods = mutableMapOf<String, OperatingPeriod>()
        for (operatingPeriod in registry.uicOperatingPeriodRegistry.values) {
            val validDays = operatingPeriod.validDayBits.map { it == '1' }
            val fromCache = operatingPeriodCache.find {
                it.fromDate == operatingPeriod.fromDate
                    && it.toDate == operatingPeriod.toDate
                    && it.validDays == validDays
            }
            if (fromCache != null) {
                operatingPeriods[operatingPeriod.id] = fromCache
                continue
            }
            val foundOrNew = operatingPeriodJpaRepository
                .findByLineVersionIdAndValidDays(
                    fromDate = operatingPeriod.fromDate,
                    toDate = operatingPeriod.toDate,
                    validDays = validDays,
                ).orElseGet { OperatingPeriod(
                    relationalId = null,
                    fromDate = operatingPeriod.fromDate,
                    toDate = operatingPeriod.toDate,
                    validDays = validDays,
                ) }
            operatingPeriodCache.add(foundOrNew)
            operatingPeriods[operatingPeriod.id] = foundOrNew
        }
        return operatingPeriods
    }
}