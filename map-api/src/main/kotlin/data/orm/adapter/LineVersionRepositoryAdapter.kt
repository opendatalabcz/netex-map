package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import org.springframework.stereotype.Component

@Component
class LineVersionRepositoryAdapter(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
): LineVersionRepository {
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
            timezone = lineVersion.validIn.timezone,
            isDetour = lineVersion.isDetour,
        )
        if (optionalSaved.isPresent) {
            return optionalSaved.get()
        }
        val mapped = toDb(lineVersion, optionalSaved.map { it.relationalId }.orElse(null))
        return lineVersionJpaRepository.save(mapped)
    }

    override fun save(lineVersion: LineVersion) {
        findSaveMapping(lineVersion)
    }

    override fun findById(lineId: LineId, validRange: DateRange, isDetour: Boolean): LineVersion? {
        return lineVersionJpaRepository
            .findByLineIdAndValidRange(
                lineExternalId = lineId.value,
                validFrom = validRange.from,
                validTo = validRange.to,
                timezone = validRange.timezone,
                isDetour = isDetour,
            ).map(::toDomain)
            .orElse(null)
    }
}
