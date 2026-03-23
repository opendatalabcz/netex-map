package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface OperatingPeriodJpaRepository: JpaRepository<OperatingPeriod, Long> {
    @Query("SELECT op FROM OperatingPeriod op " +
        "WHERE op.fromDate = :fromDate AND " +
            "op.toDate = :toDate AND " +
            "op.validDays = :validDays"
    )
    fun findByLineVersionIdAndValidDays(
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        validDays: List<Boolean>,
    ): Optional<OperatingPeriod>
}
