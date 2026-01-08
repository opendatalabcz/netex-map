package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import org.springframework.stereotype.Component

@Component
class LineVersionMapper(
    private val lineMapper: LineMapper,
    private val lineVersionJpaRepository: LineVersionJpaRepository,
) {
    fun toDomain(lineVersion: DbLineVersion): LineVersion = LineVersion(
        line = lineMapper.toDomain(lineVersion.line),
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

    fun toDb(lineVersion: LineVersion): DbLineVersion {
        val saved = lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineVersion.line.lineId.value,
            validFrom = lineVersion.validIn.from,
            validTo = lineVersion.validIn.to,
            timezone = lineVersion.validIn.timezone
        )
        return DbLineVersion(
            relationalId = saved.map { it.relationalId }.orElse(null),
            line = lineMapper.toDb(lineVersion.line),
            name = lineVersion.name,
            shortName = lineVersion.shortName,
            transportMode = lineVersion.transportMode,
            isDetour = lineVersion.isDetour,
            validFrom = lineVersion.validIn.from,
            validTo = lineVersion.validIn.to,
            timezone = lineVersion.validIn.timezone,
        )
    }
}