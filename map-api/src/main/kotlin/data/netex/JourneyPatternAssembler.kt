package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.JourneyDirectionType
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPattern
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternId
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStop
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStopId
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.TariffStop
import cz.cvut.fit.gaierda1.data.orm.model.WithinRegionTransportBan
import cz.cvut.fit.gaierda1.data.orm.model.WithinRegionTransportBanId
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import org.rutebanken.netex.model.ServiceJourneyPattern
import org.rutebanken.netex.model.StopPointInJourneyPattern
import org.springframework.stereotype.Component

@Component
class JourneyPatternAssembler {
    fun assembleJourneyPatterns(
        registry: NetexFileRegistry,
        lineVersions: Map<String, LineVersionAssembler.LineVersionAssembleResult>,
        parseCache: TimetableParseResult,
    ): Map<String, JourneyPattern> {
        val journeyPatterns = mutableMapOf<String, JourneyPattern>()
        for ((order, serviceJourneyPattern) in registry.serviceJourneyPatternRegistry.values) {
            val lineVersionKey = serviceJourneyPattern.routeView.lineRef.value.ref
            val lineVersionResult = lineVersions[lineVersionKey]
            checkNotNull(lineVersionResult) { "Line version $lineVersionKey not found" }
            val lineVersion = lineVersionResult.lineVersion

            val fromCache = parseCache.findJourneyPattern(
                patternNumber = order,
                publicCode = lineVersion.publicCode,
                isDetour = lineVersion.isDetour,
                validFrom = lineVersion.validFrom,
                validTo = lineVersion.validTo,
            )
            if (fromCache != null) {
                journeyPatterns[serviceJourneyPattern.id] = fromCache
                continue
            }

            val assembledJourneyPattern = assembleJourneyPattern(
                serviceJourneyPattern = serviceJourneyPattern,
                patternNumber = order,
                lineVersion = lineVersion,
                registry = registry,
                tariffStops = lineVersionResult.tariffStops,
            )
            parseCache.addJourneyPattern(assembledJourneyPattern)
            journeyPatterns[serviceJourneyPattern.id] = assembledJourneyPattern
        }
        return journeyPatterns
    }

    private fun assembleJourneyPattern(
        serviceJourneyPattern: ServiceJourneyPattern,
        patternNumber: Int,
        lineVersion: LineVersion,
        registry: NetexFileRegistry,
        tariffStops: Map<String, TariffStop>,
    ): JourneyPattern {
        val directionKey = serviceJourneyPattern.directionRef.ref
        val netexDirection = registry.directionRegistry[directionKey]
        checkNotNull(netexDirection) { "Direction $directionKey not found" }
        val direction = JourneyDirectionType.valueOf(netexDirection.directionType.name)

        val patternStopsList = mutableListOf<JourneyPatternStop>()
        val transportBans = mutableListOf<WithinRegionTransportBan>()
        val journeyPattern = JourneyPattern(
            patternId = JourneyPatternId(lineVersion.relationalId, patternNumber),
            lineVersion = lineVersion,
            direction = direction,
            patternStops = patternStopsList,
            transportBans = transportBans,
            route = null,
        )

        val patternStops = assembleJourneyPatternStops(journeyPattern, serviceJourneyPattern, registry, tariffStops)
        patternStopsList.addAll(patternStops.values)
        patternStopsList.sortBy { it.stopId.stopOrder }
        transportBans.addAll(assembleTransportBans(serviceJourneyPattern, patternStops))
        return journeyPattern
    }

    private fun assembleTransportBans(
        serviceJourneyPattern: ServiceJourneyPattern,
        journeyPatternStops: Map<String, JourneyPatternStop>,
    ): List<WithinRegionTransportBan> {
        if (serviceJourneyPattern.keyList == null) return emptyList()
        val transportBans = mutableListOf<WithinRegionTransportBan>()
        val banGroups = mutableMapOf<String, MutableList<String>>()
        for (keyValue in serviceJourneyPattern.keyList.keyValue) {
            when (keyValue.key) {
                "JdfFixedCode$", "JdfFixedCodeA", "JdfFixedCodeB", "JdfFixedCodeC" -> banGroups.getOrPut(keyValue.key) { mutableListOf() }.add(keyValue.value)
                else -> println("Unexpected key in ServiceJourneyPattern KeyValue list: ${keyValue.key}")
            }

        }
        var groupNumber = 0
        for ((_, values) in banGroups) {
            for (stopKey in values) {
                val journeyPatternStop = journeyPatternStops[stopKey]
                checkNotNull(journeyPatternStop) { "Journey pattern stop $stopKey not found" }
                transportBans.add(WithinRegionTransportBan(
                    banId = WithinRegionTransportBanId(journeyPatternStop.stopId, groupNumber),
                    journeyPatternStop = journeyPatternStop,
                    journeyPattern = journeyPatternStop.journeyPattern,
                ))
            }
            groupNumber += 1
        }
        return transportBans
    }

    private fun assembleJourneyPatternStops(
        journeyPattern: JourneyPattern,
        serviceJourneyPattern: ServiceJourneyPattern,
        registry: NetexFileRegistry,
        tariffStops: Map<String, TariffStop>,
    ): Map<String, JourneyPatternStop> {
        val patternStops = mutableMapOf<String, JourneyPatternStop>()
        for (pointInJourneyPattern in serviceJourneyPattern.pointsInSequence.pointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern) {
            val stopPointInJourneyPattern = pointInJourneyPattern as? StopPointInJourneyPattern ?: error("Unexpected point in journey pattern type: ${pointInJourneyPattern::class}")
            val onwardServiceLinkKey = stopPointInJourneyPattern.onwardServiceLinkRef?.ref
            val onwardServiceLink = if (onwardServiceLinkKey == null) null else {
                val link = registry.serviceLinkRegistry[onwardServiceLinkKey]
                checkNotNull(link) { "Onward service link $onwardServiceLinkKey not found" }
                link
            }

            val scheduledStopKey = stopPointInJourneyPattern.scheduledStopPointRef.value.ref
            val tariffStop = tariffStops[scheduledStopKey]
            checkNotNull(tariffStop) { "Tariff stop $scheduledStopKey not found" }

            patternStops[stopPointInJourneyPattern.id] = JourneyPatternStop(
                stopId = JourneyPatternStopId(journeyPattern.patternId, stopPointInJourneyPattern.order.toInt() - 1),
                journeyPattern = journeyPattern,
                distanceToNextStop = onwardServiceLink?.distance?.toDouble() ?: 0.0,
                tariffOrder = tariffStop.stopId.tariffOrder,
                tariffStop = tariffStop,
                forBoarding = stopPointInJourneyPattern.isForBoarding ?: true,
                forAlighting = stopPointInJourneyPattern.isForAlighting ?: true,
                requiresOrdering = stopPointInJourneyPattern.flexiblePointProperties?.isMayBeSkipped ?: false,
                stopOnRequest = stopPointInJourneyPattern.isRequestStop ?: false,
            )
        }
        return patternStops
    }
}
