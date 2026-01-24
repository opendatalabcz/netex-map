package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Repository
interface OperatingPeriodJpaRepository: JpaRepository<DbOperatingPeriod, Long> {
    @Query("SELECT op FROM DbOperatingPeriod op " +
        "WHERE op.fromDate = :fromDate AND " +
            "op.toDate = :toDate AND " +
            "op.timezone = :timezone AND " +
            "op.validDays = :validDays"
    )
    fun findByLineVersionIdAndValidDays(
        @Param("fromDate") fromDate: LocalDateTime,
        @Param("toDate") toDate: LocalDateTime,
        @Param("timezone") timezone: ZoneId,
        @Param("validDays") validDays: List<Boolean>,
    ): Optional<DbOperatingPeriod>
}
