package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
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
): LineVersionRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toDomain(lineVersion: DbLineVersion): LineVersion = LineVersion(
        lineId = LineId(lineVersion.externalId),
        publicCode = lineVersion.publicCode,
        name = lineVersion.name,
        shortName = lineVersion.shortName,
        transportMode = lineVersion.transportMode,
        isDetour = lineVersion.isDetour,
        validIn = DateRange(
            from = lineVersion.validFrom,
            to = lineVersion.validTo,
            timezone = lineVersion.timezone,
        ),
    )

    fun toDb(lineVersion: LineVersion, relationalId: Long?): DbLineVersion = DbLineVersion(
        relationalId = relationalId,
        externalId = lineVersion.lineId.value,
        publicCode = lineVersion.publicCode,
        name = lineVersion.name,
        shortName = lineVersion.shortName,
        transportMode = lineVersion.transportMode,
        isDetour = lineVersion.isDetour,
        validFrom = lineVersion.validIn.from,
        validTo = lineVersion.validIn.to,
        timezone = lineVersion.validIn.timezone,
    )

    fun findSaveMapping(lineVersion: LineVersion): DbLineVersion {
        val optionalSaved = lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineVersion.lineId.value,
            validFrom = lineVersion.validIn.from,
            validTo = lineVersion.validIn.to,
            timezone = lineVersion.validIn.timezone
        )
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            var differ = false
            if (lineVersion.name != saved.name) {
                differ = true
                logDifference("name", saved.name, lineVersion.name, saved)
            }
            if (lineVersion.shortName != saved.shortName) {
                differ = true
                logDifference("short name", saved.shortName, lineVersion.shortName, saved)
            }
            if (lineVersion.transportMode != saved.transportMode) {
                differ = true
                logDifference("transport mode", saved.transportMode, lineVersion.transportMode, saved)
            }
            if (lineVersion.isDetour != saved.isDetour) {
                differ = true
                logDifference("detour", saved.isDetour, lineVersion.isDetour, saved)
            }
            if (lineVersion.publicCode != saved.publicCode) {
                differ = true
                logDifference("public code", saved.publicCode, lineVersion.publicCode, saved)
            }
            if (!differ) return saved
        }
        val mapped = toDb(lineVersion, optionalSaved.map { it.relationalId }.orElse(null))
        return lineVersionJpaRepository.save(mapped)
    }

    override fun save(lineVersion: LineVersion) {
        findSaveMapping(lineVersion)
    }

    override fun findById(lineId: LineId, validRange: DateRange): LineVersion? {
        return lineVersionJpaRepository
            .findByLineIdAndValidRange(
                lineExternalId = lineId.value,
                validFrom = validRange.from,
                validTo = validRange.to,
                timezone = validRange.timezone,
            ).map(::toDomain)
            .orElse(null)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbLineVersion) {
        log.warn(
            "Line version {} {}-{}({}): {} changed from {} to {}",
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
