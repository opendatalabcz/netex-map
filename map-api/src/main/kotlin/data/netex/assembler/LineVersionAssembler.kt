package cz.cvut.fit.gaierda1.data.netex.assembler

import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class LineVersionAssembler {
    fun assembleLineVersion(registry: NetexFileRegistry): Map<LineId, LineVersion> {
        val lineVersions = mutableMapOf<LineId, LineVersion>()
        for (line in registry.lineRegistry.values) {
            val lineId = LineId(line.id)
            lineVersions[lineId] = LineVersion(
                line = Line(lineId, line.publicCode),
                name = line.name.value,
                shortName = line.shortName.value,
                transportMode = line.transportMode.value(),
                validIn = DateRange(
                    from = line.validBetween.first().fromDate,
                    to = line.validBetween.first().toDate,
                    timezone = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone),
                ),
                isDetour = line.keyList.keyValue.first { it.key == "JdfDetourTimetable" }?.value == "1",
            )
        }
        return lineVersions
    }
}
