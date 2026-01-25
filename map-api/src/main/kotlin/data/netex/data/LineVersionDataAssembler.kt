package cz.cvut.fit.gaierda1.data.netex.data

import cz.cvut.fit.gaierda1.data.netex.NetexFileRegistry
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class LineVersionDataAssembler(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
) {
    fun assembleLineVersions(registry: NetexFileRegistry): Map<String, DbLineVersion> {
        val lineVersions = mutableMapOf<String, DbLineVersion>()
        for (line in registry.lineRegistry.values) {
            val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
            val validFrom = line.validBetween.first().fromDate
            val validTo = line.validBetween.first().toDate
            val isDetour = line.keyList.keyValue.first { it.key == "JdfDetourTimetable" }?.value == "1"
            lineVersions[line.id] = lineVersionJpaRepository
                .findByLineIdAndValidRange(
                    lineExternalId = line.id,
                    validFrom = validFrom,
                    validTo = validTo,
                    timezone = zoneId,
                    isDetour = isDetour,
                ).orElseGet { DbLineVersion(
                        relationalId = null,
                        externalId = line.id,
                        publicCode = line.publicCode,
                        name = line.name.value,
                        shortName = line.shortName.value,
                        transportMode = line.transportMode.value(),
                        validFrom = validFrom,
                        validTo = validTo,
                        timezone = zoneId,
                        isDetour = isDetour,
                ) }
        }
        return lineVersions
    }
}