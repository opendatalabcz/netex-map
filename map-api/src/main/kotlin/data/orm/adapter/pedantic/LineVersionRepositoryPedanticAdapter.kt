package cz.cvut.fit.gaierda1.data.orm.adapter.pedantic

import cz.cvut.fit.gaierda1.data.orm.adapter.LineVersionRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LineVersionRepositoryPedanticAdapter(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
): LineVersionRepositoryAdapter(lineVersionJpaRepository) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun findOrMap(lineVersion: LineVersion): DbLineVersion {
        val optionalSaved = lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineVersion.lineId.value,
            validFrom = lineVersion.validIn.from,
            validTo = lineVersion.validIn.to,
            timezone = lineVersion.validIn.timezone,
            isDetour = lineVersion.isDetour,
        )
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            if (lineVersion.name != saved.name) {
                logDifference("name", saved.name, lineVersion.name, saved)
            }
            if (lineVersion.shortName != saved.shortName) {
                logDifference("short name", saved.shortName, lineVersion.shortName, saved)
            }
            if (lineVersion.transportMode != saved.transportMode) {
                logDifference("transport mode", saved.transportMode, lineVersion.transportMode, saved)
            }
            if (lineVersion.isDetour != saved.isDetour) {
                logDifference("detour", saved.isDetour, lineVersion.isDetour, saved)
            }
            if (lineVersion.publicCode != saved.publicCode) {
                logDifference("public code", saved.publicCode, lineVersion.publicCode, saved)
            }
            return saved
        }
        return toDb(lineVersion, null)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbLineVersion) {
        log.warn(
            "Line version {} {} - {}({}): {} changed from {} to {}",
            context.externalId,
            context.validFrom,
            context.validTo,
            context.timezone,
            fieldName,
            old,
            new
        )
    }
}
