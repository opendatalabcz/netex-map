package cz.cvut.fit.gaierda1.data.netex.domain

import cz.cvut.fit.gaierda1.data.netex.NetexFileRegistry
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class OperatingPeriodsDomainAssembler {
    fun assembleOperatingPeriods(registry: NetexFileRegistry): Map<String, OperatingPeriod> {
        val operatingPeriods = mutableMapOf<String, OperatingPeriod>()
        for (operatingPeriod in registry.uicOperatingPeriodRegistry.values) {
            val operatingPeriodId = operatingPeriod.id
            operatingPeriods[operatingPeriodId] = OperatingPeriod(
                timezone = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone),
                fromDate = operatingPeriod.fromDate,
                toDate = operatingPeriod.toDate,
                validDays = operatingPeriod.validDayBits.map { it == '1' },
            )
        }
        return operatingPeriods
    }
}