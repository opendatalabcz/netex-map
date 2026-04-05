package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class OperatingPeriodsAssembler(
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
) {
    fun assembleOperatingPeriods(
        registry: NetexFileRegistry,
        parseCache: TimetableParseResult,
    ): Map<String, OperatingPeriod> {
        val operatingPeriods = mutableMapOf<String, OperatingPeriod>()
        for (operatingPeriod in registry.uicOperatingPeriodRegistry.values) {
            val validDays = operatingPeriod.validDayBits.map { it == '1' }
            val fromCache = parseCache.findOperatingPeriod(operatingPeriod.fromDate, operatingPeriod.toDate, validDays)
            if (fromCache != null) {
                operatingPeriods[operatingPeriod.id] = fromCache
                continue
            }
            val operatingPeriodId = operatingPeriodJpaRepository.findIdByRangeAndValidDays(
                fromDate = operatingPeriod.fromDate,
                toDate = operatingPeriod.toDate,
                validDays = validDays
            )
            val assembledOperatingPeriod = OperatingPeriod(
                relationalId = operatingPeriodId.getOrNull(),
                fromDate = operatingPeriod.fromDate,
                toDate = operatingPeriod.toDate,
                validDays = validDays,
            )
            parseCache.addOperatingPeriod(assembledOperatingPeriod)
            operatingPeriods[operatingPeriod.id] = assembledOperatingPeriod
        }
        return operatingPeriods
    }
}
