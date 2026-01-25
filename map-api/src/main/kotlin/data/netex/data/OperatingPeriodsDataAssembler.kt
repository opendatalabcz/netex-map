package cz.cvut.fit.gaierda1.data.netex.data

import cz.cvut.fit.gaierda1.data.netex.NetexFileRegistry
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class OperatingPeriodsDataAssembler(
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
) {
    fun assembleOperatingPeriods(registry: NetexFileRegistry): Map<String, DbOperatingPeriod> {
        val operatingPeriods = mutableMapOf<String, DbOperatingPeriod>()
        for (operatingPeriod in registry.uicOperatingPeriodRegistry.values) {
            val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
            val validDays = operatingPeriod.validDayBits.map { it == '1' }
            operatingPeriods[operatingPeriod.id] = operatingPeriodJpaRepository
                .findByLineVersionIdAndValidDays(
                    fromDate = operatingPeriod.fromDate,
                    toDate = operatingPeriod.toDate,
                    timezone = zoneId,
                    validDays = validDays,
                ).orElseGet { DbOperatingPeriod(
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