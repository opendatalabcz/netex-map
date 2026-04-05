package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.Operator
import cz.cvut.fit.gaierda1.data.orm.repository.OperatorJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class OperatorAssembler(
    private val operatorJpaRepository: OperatorJpaRepository,
) {
    fun assembleOperators(
        registry: NetexFileRegistry,
        parseCache: TimetableParseResult,
    ): Map<String, Operator> {
        val operators = mutableMapOf<String, Operator>()
        for (operator in registry.operatorRegistry.values) {
            val fromCache = parseCache.findOperator(operator.publicCode.value)
            if (fromCache != null) {
                operators[operator.id] = fromCache
                continue
            }
            val operatorId = operatorJpaRepository.findIdByPublicCode(operator.publicCode.value)
            val assembledOperator = Operator(
                relationalId = operatorId.getOrNull(),
                publicCode = operator.publicCode.value,
                legalName = operator.legalName.value,
                phone = operator.contactDetails.phone,
                email = operator.contactDetails.email,
                url = operator.contactDetails.url,
                addressLine = operator.address.addressLine1.value,
            )
            parseCache.addOperator(assembledOperator)
            operators[operator.id] = assembledOperator
        }
        return operators
    }
}
