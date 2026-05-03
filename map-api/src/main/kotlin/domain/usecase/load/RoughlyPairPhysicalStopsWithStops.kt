package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.domain.usecase.load.RoughlyPairPhysicalStopsWithStopsUseCase.StopPair
import cz.cvut.fit.gaierda1.domain.usecase.load.nametree.NameTree
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RoughlyPairPhysicalStopsWithStops(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val stopJpaRepository: StopJpaRepository,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
): RoughlyPairPhysicalStopsWithStopsUseCase {
    private val log = LoggerFactory.getLogger(RoughlyPairPhysicalStopsWithStops::class.java)

    override fun pairPhysicalStops(
        normalizeStopNameUseCase: NormalizeStopNameUseCase,
    ): List<StopPair> {
        val resultPairs = mutableListOf<StopPair>()
        val publicCodes = lineVersionJpaRepository.findAllPublicCodes()
        val physicalStopNamesTree = NameTree(physicalStopJpaRepository.findAllDistinctNames())
        val notMatchedLines = mutableListOf<String>()
        var stopCount = 0
        var notMatchedStopCount = 0
        var ambiguousStopCount = 0
        for (publicCode in publicCodes) {
            val dbStops = stopJpaRepository
                .findAllPositionEnrichmentDtoByLinePublicCode(publicCode)
                .map { it.copy(name = normalizeStopNameUseCase.normalize(it.name)) }
            stopCount += dbStops.size
            var notMatched = 0
            for (stop in dbStops) {
                var nameMatches = physicalStopNamesTree.findAllPreciseMatches(stop.name)
                if (nameMatches.isEmpty()) {
                    nameMatches = physicalStopNamesTree.findAllSimilar(stop.name)
                }
                if (nameMatches.size == 1) {
//                    val physicalStop = physicalStopJpaRepository.findOneByName(nameMatches.first())
//                    if (physicalStop.isPresent) {
//                        resultPairs.add(StopPair(physicalStop.get(), stop))
//                    }
//                    if (stop.name != nameMatches.first()) {
//                        println("${stop.name} matched: ${nameMatches.first()}")
//                    }
                    continue
                } else if (nameMatches.size > 1) {
                    ++ambiguousStopCount
                    continue
                }
                ++notMatchedStopCount
                ++notMatched
//                if (nameMatches.isNotEmpty() && stop.name != "Praha,ÚAN Florenc") {
//                    println("${stop.name} || Candidates: ${nameMatches.joinToString(" | ")}")
//                } else {
//                    println(stop.name)
//                }
            }
            if (notMatched > 0) {
                notMatchedLines.add(publicCode)
//                log.warn("Line $publicCode has $notMatched/${dbStops.size} unmatched stops")
            }
        }
        if (notMatchedLines.isNotEmpty()) {
            val notMatchedInternationalLines = publicCodes.filter { lineVersionJpaRepository.areLinesByPublicCodeInternational(it) }
            log.warn("There are $notMatchedStopCount stops out of $stopCount that weren't found.\n" +
                    "There are $ambiguousStopCount stops out of $stopCount that weren't uniquely matched.")
            log.warn("There are ${notMatchedLines.size} lines out of ${publicCodes.size} that weren't fully matched.\n" +
                    "Out of those, ${notMatchedInternationalLines.size} are international lines.")
        }
        return resultPairs
    }
}
