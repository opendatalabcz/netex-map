package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class CalculateLineVersionActivePeriods(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val transactionTemplate: TransactionTemplate
): CalculateLineVersionActivePeriodsUseCase {
    private val lineVersionComparator: Comparator<LineVersion> = compareBy<LineVersion> { it.validFrom }
        .thenBy { it.validTo }
        .thenBy { it.isDetour }

    private fun calculateActivePeriodsForGroup(lineVersions: List<LineVersion>) {
        val sortedLineVersions = ArrayList(lineVersions.sortedWith(lineVersionComparator))
        for (i in 0 until (sortedLineVersions.size - 1)) {
            val curr = sortedLineVersions[i]
            val next = sortedLineVersions[i + 1]
            if (curr.validFrom < next.validFrom) {
                curr.activeFrom = curr.validFrom
                curr.activeTo = if (curr.validTo < next.validFrom) curr.validTo else next.validFrom
            }
            // curr.validFrom == next.validFrom because of sorting
            // curr isn't active in any day because
            // - it either defines a shorter period
            // - or the periods are the same, but the next one is a detour
        }
        val last = sortedLineVersions.last()
        last.activeFrom = last.validFrom
        last.activeTo = last.validTo
    }

    override fun calculateActivePeriods() {
        val codePageSize = 100
        var codePageNumber = 0
        var publicCodes: Page<String>? = null
        do { transactionTemplate.executeWithoutResult {
            publicCodes = lineVersionJpaRepository.findAllPublicCodes(PageRequest.of(codePageNumber, codePageSize))
            val lineVersions = lineVersionJpaRepository.findAllByPublicCodes(publicCodes.content)
            for ((_, lineVersionGroup) in lineVersions.groupBy { it.publicCode }) {
                calculateActivePeriodsForGroup(lineVersionGroup)
            }
            lineVersionJpaRepository.saveAll(lineVersions)
            ++codePageNumber
        } } while (publicCodes?.hasNext() ?: false)
    }
}