package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.model.ActivePeriod
import cz.cvut.fit.gaierda1.data.orm.model.ActivePeriodId
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.ActivePeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime

@Component
class CalculateLineVersionActivePeriods(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val activePeriodJpaRepository: ActivePeriodJpaRepository,
    private val transactionTemplate: TransactionTemplate,
): CalculateLineVersionActivePeriodsUseCase {
    private fun calculateActivePeriodsForGroup(lineVersions: List<LineVersion>): List<ActivePeriod> {
        val validVersions = lineVersions.filter { it.validFrom.isBefore(it.validTo) }
        if (validVersions.isEmpty()) return emptyList()

        val timePoints = validVersions
            .flatMap { listOf(it.validFrom, it.validTo) }
            .distinctBy(OffsetDateTime::toInstant)
            .sorted()

        val fractionalActivePeriods = mutableListOf<ActivePeriod>()

        for (i in 0 until timePoints.size - 1) {
            val segmentStart = timePoints[i]
            val segmentEnd = timePoints[i + 1]

            val activeCandidates = validVersions.filter {
                !it.validFrom.isAfter(segmentStart) && !it.validTo.isBefore(segmentEnd)
            }

            if (activeCandidates.isNotEmpty()) {
                val winner = activeCandidates.maxWith(
                    compareBy<LineVersion> { it.validFrom }
                        .thenBy { it.isDetour }
                )

                fractionalActivePeriods.add( ActivePeriod(
                    ActivePeriodId(winner.relationalId, segmentStart),
                    winner,
                    segmentEnd,
                ) )
            }
        }

        val mergedPeriods = mutableListOf<ActivePeriod>()
        for (fractionalPeriod in fractionalActivePeriods) {
            val lastMerged = mergedPeriods.lastOrNull()

            if (lastMerged != null
                && lastMerged.periodId.lineVersionId == fractionalPeriod.periodId.lineVersionId
                && lastMerged.toDate.isEqual(fractionalPeriod.periodId.fromDate)
            ) {
                mergedPeriods[mergedPeriods.lastIndex] = ActivePeriod(
                    lastMerged.periodId,
                    lastMerged.lineVersion,
                    fractionalPeriod.toDate,
                )
            } else {
                mergedPeriods.add(fractionalPeriod)
            }
        }

        return mergedPeriods
    }

    override fun calculateActivePeriods() {
        val codePageSize = 100
        var codePageNumber = 0
        var publicCodes: Page<String>? = null
        activePeriodJpaRepository.deleteAll()
        do { transactionTemplate.executeWithoutResult {
            publicCodes = lineVersionJpaRepository.findAllPublicCodes(PageRequest.of(codePageNumber, codePageSize))
            val lineVersions = lineVersionJpaRepository.findAllByPublicCodes(publicCodes.content)
            for ((_, lineVersionGroup) in lineVersions.groupBy { it.publicCode }) {
                val activePeriods = calculateActivePeriodsForGroup(lineVersionGroup)
                activePeriodJpaRepository.saveAll(activePeriods)
            }
            ++codePageNumber
        } } while (publicCodes?.hasNext() ?: false)
    }
}