package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.ActivePeriod
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ActivePeriodDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ActivePeriodForSingleLineDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ActivePeriodJpaRepository: JpaRepository<ActivePeriod, Long> {
    @Query(nativeQuery = true, value = """
        SELECT ap.from_date, ap.to_date
        FROM active_period ap
        WHERE ap.line_version_id = :lineVersionId
    """)
    fun findAllDtoForSingleLineByLineVersionId(lineVersionId: Long): List<ActivePeriodForSingleLineDto>

    @Query(nativeQuery = true, value = """
        SELECT ap.line_version_id, ap.from_date, ap.to_date
        FROM active_period ap
        WHERE ap.line_version_id IN :lineVersionIds
    """)
    fun findAllDtoByLineVersionIds(lineVersionIds: List<Long>): List<ActivePeriodDto>
}
