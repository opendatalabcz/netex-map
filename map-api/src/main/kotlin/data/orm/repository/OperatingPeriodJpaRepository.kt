package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatingPeriodDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface OperatingPeriodJpaRepository: JpaRepository<OperatingPeriod, Long> {
    @Query("SELECT op.relationalId FROM OperatingPeriod op " +
        "WHERE op.fromDate = :fromDate AND " +
            "op.toDate = :toDate AND " +
            "op.validDays = :validDays"
    )
    fun findIdByRangeAndValidDays(
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        validDays: List<Boolean>,
    ): Optional<Long>

    @Query(nativeQuery = true, value = """
        SELECT DISTINCT op.relational_id, op.from_date, op.to_date, op.valid_days
        FROM operating_period op
        WHERE op.relational_id IN :operatingPeriodIds
    """)
    fun findAllWallDtoByIds(operatingPeriodIds: List<Long>): List<OperatingPeriodDto>
}
