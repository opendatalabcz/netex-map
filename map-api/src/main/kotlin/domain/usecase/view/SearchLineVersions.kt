package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.ActivePeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatorJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ActivePeriodDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ActivePeriodForSingleLineDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatorDto
import cz.cvut.fit.gaierda1.domain.usecase.view.SearchLineVersionsUseCase.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class SearchLineVersions(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatorJpaRepository: OperatorJpaRepository,
    private val activePeriodJpaRepository: ActivePeriodJpaRepository,
): SearchLineVersionsUseCase {
    private fun escapedQuery(query: String): String {
        return query.replace("\\", "\\\\")
            .replace("_", "\\_")
            .replace("%", "\\%")
    }

    override fun search(query: String, pageable: Pageable?): Page<SearchLineVersion> {
        val escapedQuery = escapedQuery(query)
        val lineVersions = lineVersionJpaRepository.searchDto(escapedQuery, pageable ?: PageRequest.of(0, 20))
        val operators = operatorJpaRepository
            .findAllDtoByOperatorIds(lineVersions.content.map(LineVersionDto::operatorId))
            .associateBy(OperatorDto::relationalId)
        val activePeriods = activePeriodJpaRepository
            .findAllDtoByLineVersionIds(lineVersions.content.map(LineVersionDto::relationalId))
            .groupBy(ActivePeriodDto::lineVersionId)
            .mapValues { (_, periods) ->
                periods.map { ActivePeriodForSingleLineDto(it.fromDate, it.toDate) }
            }
        return lineVersions.map { lineVersions -> SearchLineVersion(
            relationalId = lineVersions.relationalId,
            publicCode = lineVersions.publicCode,
            name = lineVersions.name,
            shortName = lineVersions.shortName,
            transportMode = lineVersions.transportMode,
            lineType = lineVersions.lineType,
            isDetour = lineVersions.isDetour,
            validFrom = lineVersions.validFrom,
            validTo = lineVersions.validTo,
            operator = operators[lineVersions.operatorId]!!,
            activePeriods = activePeriods[lineVersions.relationalId] ?: emptyList(),
        ) }
    }
}