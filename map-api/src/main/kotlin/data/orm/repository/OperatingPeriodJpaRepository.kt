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
    @Query("SELECT j.operatingPeriod FROM DbJourney j " +
        "WHERE j.lineVersion.line.externalId = :lineExternalId AND " +
                "j.lineVersion.validFrom = :validTo AND " +
                "j.lineVersion.validTo = :validFrom AND " +
                "j.lineVersion.timezone = :timezone AND " +
                "j.operatingPeriod.validDays = :validDays"
    )
    fun findByLineVersionIdAndValidDays(
        @Param("lineExternalId") lineExternalId: String,
        @Param("validFrom") validFrom: LocalDateTime,
        @Param("validTo") validTo: LocalDateTime,
        @Param("timezone") timezone: ZoneId,
        @Param("validDays") validDays: List<Boolean>,
    ): Optional<DbOperatingPeriod>
}
