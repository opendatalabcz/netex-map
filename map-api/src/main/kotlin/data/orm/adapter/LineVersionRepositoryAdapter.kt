package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import org.springframework.stereotype.Component

@Component
open class LineVersionRepositoryAdapter(
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

     fun findOrMap(lineVersion: LineVersion): DbLineVersion {
        val optionalSaved = lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineVersion.lineId.value,
            validFrom = lineVersion.validIn.from,
            validTo = lineVersion.validIn.to,
            timezone = lineVersion.validIn.timezone,
            isDetour = lineVersion.isDetour,
        )
        return optionalSaved.orElseGet { toDb(lineVersion, null) }
    }

    fun saveDb(lineVersion: DbLineVersion) {
        lineVersionJpaRepository.save(lineVersion)
    }

    fun saveAllDb(lineVersions: Iterable<DbLineVersion>) {
        lineVersionJpaRepository.saveAll(lineVersions)
    }

    fun findSaveMapping(lineVersion: LineVersion): DbLineVersion {
        val mapped = findOrMap(lineVersion)
        if (mapped.relationalId == null) saveDb(mapped)
        return mapped
    }

    private val lineVersionComparator = compareBy<LineVersion> { it.lineId.value }
        .thenBy { it.validIn.from }
        .thenBy { it.validIn.from }
        .thenBy { it.isDetour }
        .thenBy { it.validIn.timezone.id }

    private fun findSaveMappingsImpl(lineVersions: Iterable<LineVersion>, result: Boolean): List<DbLineVersion>? {
        val uniqueLineVersions = sortedSetOf(comparator = lineVersionComparator)
        uniqueLineVersions.addAll(lineVersions)
        val mappedUniqueLineVersions = uniqueLineVersions.map(::findOrMap)
        saveAllDb(mappedUniqueLineVersions.filter { it.relationalId == null })
        return if (result) lineVersions.map { domainLineVersion -> mappedUniqueLineVersions.find { dbLineVersion ->
                    domainLineVersion.lineId.value == dbLineVersion.externalId
                        && domainLineVersion.validIn.from.equals(dbLineVersion.validFrom)
                        && domainLineVersion.validIn.to.equals(dbLineVersion.validTo)
                        && domainLineVersion.isDetour == dbLineVersion.isDetour
                        && domainLineVersion.validIn.timezone.id == dbLineVersion.timezone.id
                }!! }
            else null
    }

    fun findSaveMappings(lineVersions: Iterable<LineVersion>): List<DbLineVersion> {
        return findSaveMappingsImpl(lineVersions, true)!!
    }

    override fun saveIfAbsent(lineVersion: LineVersion) {
        findSaveMapping(lineVersion)
    }

    override fun saveAllIfAbsent(lineVersions: Iterable<LineVersion>) {
        findSaveMappingsImpl(lineVersions, false)
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
