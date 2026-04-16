package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.LineType
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.Operator
import cz.cvut.fit.gaierda1.data.orm.model.Stop
import cz.cvut.fit.gaierda1.data.orm.model.TariffStop
import cz.cvut.fit.gaierda1.data.orm.model.TariffStopId
import cz.cvut.fit.gaierda1.data.orm.model.TransportMode
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.misc.atOffsetByZoneId
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import org.rutebanken.netex.model.Line
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class LineVersionAssembler(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
) {
    data class LineVersionAssembleResult(
        val lineVersion: LineVersion,
        val tariffStops: Map<String, TariffStop>,
    )

    fun assembleLineVersions(
        registry: NetexFileRegistry,
        operators: Map<String, Operator>,
        stops: Map<String, Stop>,
        parseCache: TimetableParseResult,
    ): Map<String, LineVersionAssembleResult> {
        val lineVersions = mutableMapOf<String, LineVersionAssembleResult>()
        val zoneId = ZoneId.of(registry.frameDefaults.defaultLocale.timeZone)
        for (line in registry.lineRegistry.values) {
            val validFrom = line.validBetween.first().fromDate.atOffsetByZoneId(zoneId)
            val validTo = line.validBetween.first().toDate.atOffsetByZoneId(zoneId)
            val isDetour = line.keyList.keyValue.firstOrNull { it.key == "JdfDetourTimetable" }?.value == "1"

            val fromCache = parseCache.findLineVersion(line.publicCode, isDetour, validFrom, validTo)
            if (fromCache != null) {
                continue
            }

            val lineVersionId = lineVersionJpaRepository.findIdByPublicCodeAndValidRangeAndDetour(
                publicCode = line.publicCode,
                validFrom = validFrom,
                validTo = validTo,
                isDetour = isDetour,
            )
            val assembledLineVersion = assembleLineVersion(
                line = line,
                registry = registry,
                operators = operators,
                stops = stops,
                validFrom = validFrom,
                validTo = validTo,
                isDetour = isDetour,
            )
            lineVersionId.ifPresent {
                assembledLineVersion.lineVersion.relationalId = it
            }
            parseCache.addLineVersion(assembledLineVersion.lineVersion)
            lineVersions[line.id] = assembledLineVersion
        }
        return lineVersions
    }

    private fun assembleLineVersion(
        line: Line,
        registry: NetexFileRegistry,
        operators: Map<String, Operator>,
        stops: Map<String, Stop>,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
        isDetour: Boolean,
    ): LineVersionAssembleResult {
        val lineType = LineType.fromJdfCode(line.keyList.keyValue.firstOrNull { it.key == "JdfLineType" }?.value ?: "A")
        val operator = linkOperator(line, operators)
        val tariffStopsList = mutableListOf<TariffStop>()
        val lineVersion = LineVersion(
            relationalId = null,
            publicCode = line.publicCode,
            name = line.name.value,
            shortName = line.shortName.value,
            transportMode = TransportMode.fromNetexTransportMode(line.transportMode),
            lineType = lineType,
            validFrom = validFrom,
            validTo = validTo,
            isDetour = isDetour,
            operator = operator,
            activePeriods = emptyList(),
            tariffStops = tariffStopsList,
        )
        val tariffStops = assembleTariffStops(lineVersion, registry, stops)
        tariffStopsList.addAll(tariffStops.values)
        tariffStopsList.sortBy { it.stopId.tariffOrder }
        return LineVersionAssembleResult(lineVersion, tariffStops)
    }

    private fun assembleTariffStops(
        lineVersion: LineVersion,
        registry: NetexFileRegistry,
        stops: Map<String, Stop>,
    ): Map<String, TariffStop> {
        if (registry.lineRegistry.size != 1) error("Expected exactly one line, found ${registry.lineRegistry.size}")
        val tariffStops = mutableMapOf<String, TariffStop>()
        val orderedScheduledStopPoints = registry.scheduledStopPointRegistry.values.toMutableList()
        orderedScheduledStopPoints.sortBy { it.first }
        for ((idx, scheduledStopPoint) in orderedScheduledStopPoints) {
            val tariffZones = scheduledStopPoint.tariffZones?.tariffZoneRef_
            val tariffZoneName = if (tariffZones == null) null else {
                if (tariffZones.size != 1) error("Expected exactly one tariff zone, found ${tariffZones.size}")
                val zoneKey = tariffZones.first().value.ref
                val zone = registry.tariffZoneRegistry[zoneKey]
                checkNotNull(zone) { "Tariff zone $zoneKey not found" }
                zone.name.value
            }

            val stopAssignment = registry.stopAssignmentRegistryByScheduledStopPointId[scheduledStopPoint.id]
            checkNotNull(stopAssignment) { "Stop assignment for scheduled stop point ${scheduledStopPoint.id} not found" }
            val stopKey = stopAssignment.stopPlaceRef.value.ref
            checkNotNull(stopKey) { "Stop assignment for scheduled stop point ${scheduledStopPoint.id} not found" }
            val stop = stops[stopKey]
            checkNotNull(stop) { "Stop $stopKey not found" }

            tariffStops[scheduledStopPoint.id] = TariffStop(
                stopId = TariffStopId(lineVersion.relationalId, idx),
                lineVersion = lineVersion,
                tariffZone = tariffZoneName,
                stopPlace = stop,
            )
        }
        return tariffStops
    }

    private fun linkOperator(
        line: Line,
        operators: Map<String, Operator>,
    ): Operator {
        val operatorRef = line.operatorRef.ref
        val operator = operators[operatorRef]
        checkNotNull(operator) { "Operator $operatorRef not found" }
        return operator
    }
}
