package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.LineVersionMapper
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LineVersionRepositoryAdapter(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val lineVersionMapper: LineVersionMapper,
): LineVersionRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun save(lineVersion: LineVersion) {
        val mapped = lineVersionMapper.toDb(lineVersion)
        val optionalSaved = lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineVersion.line.lineId.value,
            validFrom = lineVersion.validIn.from,
            validTo = lineVersion.validIn.to,
            timezone = lineVersion.validIn.timezone
        )
        optionalSaved.ifPresent { saved ->
            mapped.relationalId = saved.relationalId
            checkAndLogDifference("name", saved.name, lineVersion.name, lineVersion)
            checkAndLogDifference("short name", saved.shortName, lineVersion.shortName, lineVersion)
            checkAndLogDifference("transport mode", saved.transportMode, lineVersion.transportMode, lineVersion)
            checkAndLogDifference("detour", saved.isDetour, lineVersion.isDetour, lineVersion)
        }
        lineVersionJpaRepository.save(mapped)
    }

    override fun findById(lineId: LineId, validRange: DateRange): LineVersion? {
        return lineVersionJpaRepository
            .findByLineIdAndValidRange(
                lineExternalId = lineId.value,
                validFrom = validRange.from,
                validTo = validRange.to,
                timezone = validRange.timezone,
            ).map(lineVersionMapper::toDomain)
            .orElse(null)
    }

    private fun checkAndLogDifference(fieldName: String, old: Any?, new: Any?, context: LineVersion) {
        if (old != new) {
            log.warn(
                "Line version {} {}-{}({}): {} changed from {} to {}",
                context.line.lineId.value,
                context.validIn.from,
                context.validIn.to,
                context.validIn.timezone,
                fieldName,
                old,
                new
            )
        }
    }
}
