package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.misc.atOffset
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class LineVersionAssembler(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
) {
    fun assembleLineVersions(registry: NetexFileRegistry): Map<String, LineVersion> {
        val lineVersions = mutableMapOf<String, LineVersion>()
        val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
        for (line in registry.lineRegistry.values) {
            val validFrom = line.validBetween.first().fromDate.atOffset(zoneId)
            val validTo = line.validBetween.first().toDate.atOffset(zoneId)
            val isDetour = line.keyList.keyValue.first { it.key == "JdfDetourTimetable" }?.value == "1"
            lineVersions[line.id] = lineVersionJpaRepository
                .findByLineIdAndValidRange(
                    lineExternalId = line.id,
                    validFrom = validFrom,
                    validTo = validTo,
                    isDetour = isDetour,
                ).orElseGet { LineVersion(
                    relationalId = null,
                    externalId = line.id,
                    publicCode = line.publicCode,
                    name = line.name.value,
                    shortName = line.shortName.value,
                    transportMode = line.transportMode.value(),
                    validFrom = validFrom,
                    validTo = validTo,
                    isDetour = isDetour,
                    activeFrom = null,
                    activeTo = null,
                ) }
        }
        return lineVersions
    }
}