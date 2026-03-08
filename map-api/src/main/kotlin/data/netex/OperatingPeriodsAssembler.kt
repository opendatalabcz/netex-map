package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class OperatingPeriodsAssembler(
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
) {
    fun assembleOperatingPeriods(registry: NetexFileRegistry): Map<String, OperatingPeriod> {
        val operatingPeriods = mutableMapOf<String, OperatingPeriod>()
        for (operatingPeriod in registry.uicOperatingPeriodRegistry.values) {
            val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
            val validDays = operatingPeriod.validDayBits.map { it == '1' }
            operatingPeriods[operatingPeriod.id] = operatingPeriodJpaRepository
                .findByLineVersionIdAndValidDays(
                    fromDate = operatingPeriod.fromDate,
                    toDate = operatingPeriod.toDate,
                    timezone = zoneId,
                    validDays = validDays,
                ).orElseGet { OperatingPeriod(
                    relationalId = null,
                    timezone = zoneId,
                    fromDate = operatingPeriod.fromDate,
                    toDate = operatingPeriod.toDate,
                    validDays = validDays,
                ) }
        }
        return operatingPeriods
    }
}