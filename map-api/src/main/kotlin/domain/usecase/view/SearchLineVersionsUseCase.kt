package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.dto.ActivePeriodForSingleLineDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatorDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant

interface SearchLineVersionsUseCase {
    data class SearchLineVersion(
        val relationalId: Long,
        val publicCode: String,
        val name: String,
        val shortName: String,
        val transportMode: String,
        val lineType: String,
        val isDetour: Boolean,
        val validFrom: Instant,
        val validTo: Instant,
        val operator: OperatorDto,
        val activePeriods: List<ActivePeriodForSingleLineDto>,
    )
    fun search(query: String, pageable: Pageable?): Page<SearchLineVersion>
}