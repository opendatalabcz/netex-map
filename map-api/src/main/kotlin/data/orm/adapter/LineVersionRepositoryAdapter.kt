package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbLine
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LineVersionRepositoryAdapter(
    private val lineJpaRepository: LineJpaRepository,
    private val lineVersionJpaRepository: LineVersionJpaRepository,
): LineVersionRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toDomain(line: DbLine): Line = Line(
        lineId = LineId(line.externalId),
        publicCode = line.publicCode,
    )

    fun toDomain(lineVersion: DbLineVersion): LineVersion = LineVersion(
        line = toDomain(lineVersion.line),
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

    fun toDb(line: Line, relationalId: Long?): DbLine = DbLine(
        relationalId = relationalId,
        externalId = line.lineId.value,
        publicCode = line.publicCode,
    )

    fun toDb(lineVersion: LineVersion, relationalId: Long?, line: DbLine): DbLineVersion = DbLineVersion(
        relationalId = relationalId,
        line = line,
        name = lineVersion.name,
        shortName = lineVersion.shortName,
        transportMode = lineVersion.transportMode,
        isDetour = lineVersion.isDetour,
        validFrom = lineVersion.validIn.from,
        validTo = lineVersion.validIn.to,
        timezone = lineVersion.validIn.timezone,
    )

    fun findSaveMapping(line: Line): DbLine {
        val optionalSaved = lineJpaRepository.findByExternalId(line.lineId.value)
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            if (line.publicCode == saved.publicCode) return saved
            else logDifference("public code", saved.publicCode, line.publicCode, saved)
        }
        val mapped = toDb(line, optionalSaved.map { it.relationalId }.orElse(null))
        return lineJpaRepository.save(mapped)
    }

    fun findSaveMapping(lineVersion: LineVersion): DbLineVersion {
        val optionalSaved = lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineVersion.line.lineId.value,
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
            if (lineVersion.line.publicCode != saved.line.publicCode) {
                differ = true
                // the difference will be logged in the findSaveMapping of the line
            }
            if (!differ) return saved
        }
        val mapped = toDb(lineVersion, optionalSaved.map { it.relationalId }.orElse(null), findSaveMapping(lineVersion.line))
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

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbLine) {
        log.warn(
            "Line {}: {} changed from {} to {}",
            context.externalId,
            fieldName,
            old,
            new
        )
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbLineVersion) {
        log.warn(
            "Line version {} {}-{}({}): {} changed from {} to {}",
            context.line.externalId,
            context.validFrom,
            context.validTo,
            context.timezone,
            fieldName,
            old,
            new
        )
    }
}
