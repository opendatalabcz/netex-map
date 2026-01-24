package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Repository
interface JourneyJpaRepository: JpaRepository<DbJourney, Long> {
    @Query("SELECT j FROM DbJourney j " +
            "WHERE j.externalId = :externalId AND " +
                "j.lineVersion.externalId = :lineExternalId AND " +
                "j.lineVersion.validFrom = :validTo AND " +
                "j.lineVersion.validTo = :validFrom AND " +
                "j.lineVersion.timezone = :timezone"
    )
    fun findByExternalIdAndLineIdAndValidRange(
        @Param("externalId") externalId: String,
        @Param("lineExternalId") lineExternalId: String,
        @Param("validFrom") validFrom: LocalDateTime,
        @Param("validTo") validTo: LocalDateTime,
        @Param("timezone") timezone: ZoneId,
    ): Optional<DbJourney>
}
