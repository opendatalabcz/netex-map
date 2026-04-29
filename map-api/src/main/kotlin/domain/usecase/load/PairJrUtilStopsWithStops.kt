package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.StopPositionEnrichmentDto
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.*
import cz.cvut.fit.gaierda1.domain.usecase.load.PairJrUtilStopsWithStopsUseCase.PairJrUtilStopsWithStopsResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PairJrUtilStopsWithStops(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val stopJpaRepository: StopJpaRepository,
) : PairJrUtilStopsWithStopsUseCase {
    companion object {
        private const val NULL_NAME = "__NULL_NAME__"
    }
    private val log = LoggerFactory.getLogger(PairJrUtilStopsWithStops::class.java)

    private fun String.removeCommasAndSpaces(): String = replace(Regex("[ ,]"), "")
    private fun List<StopPositionEnrichmentDto>.orderedByNamePartCountDesc(): List<StopPositionEnrichmentDto> =
        sortedByDescending { it.name.split(",").size }

    private data class MatchedStop(
        val stop: StopPositionEnrichmentDto,
        var matchResult: JrUtilStopsByName?,
    )

    private data class JrUtilStopsByName(
        val name: String,
        val stops: MutableList<PhysicalStop>,
    )

    private fun bestTryMatchByName(
        stopMatches: List<MatchedStop>,
        jrUtilStops: List<JrUtilStopsByName>
    ) {
        for (stopMatch in stopMatches) {
            if (stopMatch.matchResult != null) continue
            val dbStopNameNoCS = stopMatch.stop.name.removeCommasAndSpaces()
            val partialMatches = mutableListOf<JrUtilStopsByName>()
            val noCSMatches = mutableListOf<JrUtilStopsByName>()
            val containsNoCSMatches = mutableListOf<JrUtilStopsByName>()
            for (jrStop in jrUtilStops) {
                // Skip used
                if (stopMatches.any { it.matchResult?.name == jrStop.name }) continue
                // Match partially
                val jrStopNameParts = jrStop.name.split(",")
                for (size in 1 until jrStopNameParts.size + 1) {
                    val partialName = jrStopNameParts.takeLast(size).joinToString(",")
                    if (partialName == stopMatch.stop.name) partialMatches.add(jrStop)
                }
                // Match with no commas and spaces
                val jrStopNameNoCS = jrStop.name.removeCommasAndSpaces()
                if (jrStopNameNoCS == dbStopNameNoCS) noCSMatches.add(jrStop)
                if (jrStopNameNoCS.contains(dbStopNameNoCS)) containsNoCSMatches.add(jrStop)
            }
            if (partialMatches.size == 1) stopMatch.matchResult = partialMatches.first()
            else if (noCSMatches.size == 1) stopMatch.matchResult = noCSMatches.first()
            else if (containsNoCSMatches.size == 1) stopMatch.matchResult = containsNoCSMatches.first()
        }
    }

    private fun localMatchStopsForLine(
        line: JrUtilGtfsLineParseResult,
        dbStops: List<StopPositionEnrichmentDto>,
    ): List<MatchedStop> {
        val stopMatches = dbStops.orderedByNamePartCountDesc().map { dbStop ->
            val exactMatch = line.stops.find { it.name == dbStop.name }
            MatchedStop(dbStop, exactMatch?.let { JrUtilStopsByName(it.name ?: NULL_NAME, mutableListOf(it)) })
        }
        bestTryMatchByName(stopMatches, line.stops.map { JrUtilStopsByName(it.name ?: NULL_NAME, mutableListOf(it)) })
        return stopMatches
    }

    private fun globalMatchByNameWithPrefixes(
        dbStop: StopPositionEnrichmentDto,
        stopNamePrefixes: List<String>,
        allJrUtilStopsByName: Map<String, JrUtilStopsByName>
    ): JrUtilStopsByName? {
        data class GlobalMatch(
            val jrUsageStop: JrUtilStopsByName,
            val exactMatch: Boolean,
            val noCSExactMatch: Boolean,
        )
        val matches = mutableListOf<GlobalMatch>()
        for (jrStop in allJrUtilStopsByName.values) {
            for (namePrefix in stopNamePrefixes) {
                val usedDbStopName = namePrefix + dbStop.name
                val exactMatch = jrStop.name == usedDbStopName
                val exactlyMatchesByNoCS = jrStop.name.removeCommasAndSpaces() == usedDbStopName.removeCommasAndSpaces()
                if (!exactMatch && !exactlyMatchesByNoCS) continue
                matches.add(GlobalMatch(
                    jrUsageStop = jrStop,
                    exactMatch = exactMatch,
                    noCSExactMatch = exactlyMatchesByNoCS,
                ))
            }
        }
        var exactPrefixMatch = matches.find { it.exactMatch }
        if (exactPrefixMatch == null) exactPrefixMatch = matches.find { it.noCSExactMatch }
        if (exactPrefixMatch != null) {
            return exactPrefixMatch.jrUsageStop
        }
        return null
    }

    private fun globalMatchStopWithNeighbouringContext(
        stopIdx: Int,
        stopMatches: List<MatchedStop>,
        allJrUtilStopsByName: Map<String, JrUtilStopsByName>
    ): JrUtilStopsByName? {
        val exactMatch = allJrUtilStopsByName[stopMatches[stopIdx].stop.name]
        if (exactMatch != null) {
            return exactMatch
        }
        val stopNamePrefixes = mutableSetOf("")
        for ((idx, otherStopMatch) in stopMatches.withIndex()) {
            if (idx == stopIdx || otherStopMatch.matchResult == null) continue
            val otherStopNameParts = otherStopMatch.matchResult!!.name.split(",")
            if (otherStopNameParts.size == 1) continue
            for (size in 1 until otherStopNameParts.size) {
                stopNamePrefixes.add(otherStopNameParts.take(size).joinToString(",", postfix = ","))
            }
        }
        return globalMatchByNameWithPrefixes(stopMatches[stopIdx].stop, stopNamePrefixes.toList(), allJrUtilStopsByName)
    }

    private fun globalMatchStopsForLine(
        allJrUtilStopsByName: Map<String, JrUtilStopsByName>,
        dbStops: List<StopPositionEnrichmentDto>,
    ): List<MatchedStop> {
        val stopMatches = dbStops.orderedByNamePartCountDesc().map { MatchedStop(it, allJrUtilStopsByName[it.name]) }
        bestTryMatchByName(stopMatches, allJrUtilStopsByName.values.toList())
        return stopMatches
    }

    private fun deduplicateStops(stops: List<Pair<Long, JrUtilStopsByName>>): List<Pair<Long, PhysicalStop>> {
        val deduplicatedStops = mutableListOf<Pair<Long, PhysicalStop>>()
        val toDecideStops = mutableListOf<Pair<Long, JrUtilStopsByName>>()
        for (entry in stops) {
            if (entry.second.stops.size == 1) {
                deduplicatedStops.add(entry.first to entry.second.stops.first())
            } else {
                toDecideStops.add(entry)
            }
        }
        val deduplicatedStopCoordinates = deduplicatedStops.map { it.second.position }.filter { it.x != 0.0 && it.y != 0.0 }
        val decidedStops = mutableListOf<Pair<Long, PhysicalStop>>()
        for (entry in toDecideStops) {
            var closestIndex = 0
            var closestDistance = Double.MAX_VALUE
            for ((idx, testedStop) in entry.second.stops.withIndex()) {
                if (testedStop.position.x == 0.0 || testedStop.position.y == 0.0) continue
                for (deduplicatedStopCoordinate in deduplicatedStopCoordinates) {
                    val distance = testedStop.position.distance(deduplicatedStopCoordinate)
                    if (distance < closestDistance) {
                        closestIndex = idx
                        closestDistance = distance
                    }
                }
            }
            decidedStops.add(entry.first to entry.second.stops[closestIndex])
        }
        deduplicatedStops.addAll(decidedStops)
        return deduplicatedStops
    }

    override fun pairJrUtilStopsWithStops(
        jrUtilGtfsParseResult: JrUtilGtfsParseResult,
        normalizeStopNameUseCase: NormalizeStopNameUseCase,
    ): List<PairJrUtilStopsWithStopsResult> {
        val assignmentResult = mutableListOf<PairJrUtilStopsWithStopsResult>()
        val linesByPublicCode = jrUtilGtfsParseResult.lines.associateBy { it.publicCode }
        val normalizedJrUtilStopsByName = mutableMapOf<String, JrUtilStopsByName>()
        for (stop in jrUtilGtfsParseResult.allStops) {
            val key = stop.name?.let(normalizeStopNameUseCase::normalize) ?: NULL_NAME
            val mapEntry = normalizedJrUtilStopsByName[key]
            if (mapEntry == null) {
                normalizedJrUtilStopsByName[key] = JrUtilStopsByName(key, mutableListOf(stop))
            } else {
                mapEntry.stops.add(stop)
            }
        }
        val publicCodes = lineVersionJpaRepository.findAllPublicCodes()
        var notMatchedLines = 0
        for (publicCode in publicCodes) {
            val dbStops = stopJpaRepository
                .findAllPositionEnrichmentDtoByLinePublicCode(publicCode)
                .map { it.copy(name = normalizeStopNameUseCase.normalize(it.name)) }
            val line = linesByPublicCode[publicCode]
            val stopMatches =
                if (line != null) localMatchStopsForLine(line, dbStops)
                else globalMatchStopsForLine(normalizedJrUtilStopsByName, dbStops)
            var notMatched = 0
            val tmpResult = mutableListOf<Pair<Long, JrUtilStopsByName>>()
            for ((idx, stopMatch) in stopMatches.withIndex()) {
                val matchedJrUtilStop = stopMatch.matchResult
                    ?: globalMatchStopWithNeighbouringContext(idx, stopMatches, normalizedJrUtilStopsByName)
                if (matchedJrUtilStop != null) {
                    tmpResult.add(stopMatch.stop.relationalId to matchedJrUtilStop)
                } else {
                    notMatched++
                    log.warn("Line $publicCode, stop \"${stopMatch.stop.name}\" didn't match any JrUtil stop: ${line?.stops?.joinToString { "\"${it.name}\"" }}")
                }
            }
            assignmentResult.add(PairJrUtilStopsWithStopsResult(
                linePublicCode = publicCode,
                assignmentsByStopId = deduplicateStops(tmpResult).associate { it },
            ))
            if (notMatched > 0) {
                notMatchedLines++
                log.warn("Line $publicCode has $notMatched/${dbStops.size} unmatched stops")
            }
        }
        if (notMatchedLines > 0) {
            log.warn("There are $notMatchedLines lines that weren't fully matched")
        }
        return assignmentResult
    }
}
