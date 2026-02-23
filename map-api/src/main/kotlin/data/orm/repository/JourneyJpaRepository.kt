package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
                "j.lineVersion.validFrom = :validFrom AND " +
                "j.lineVersion.validTo = :validTo AND " +
                "j.lineVersion.timezone = :timezone AND " +
                "j.lineVersion.isDetour = :isDetour"
    )
    fun findByExternalIdAndLineIdAndValidRange(
        @Param("externalId") externalId: String,
        @Param("lineExternalId") lineExternalId: String,
        @Param("validFrom") validFrom: LocalDateTime,
        @Param("validTo") validTo: LocalDateTime,
        @Param("timezone") timezone: ZoneId,
        @Param("isDetour") isDetour: Boolean,
    ): Optional<DbJourney>

    @Query("SELECT j FROM DbJourney j WHERE j.route IS NULL")
    fun findByNullRoute(pageable: Pageable): Page<DbJourney>
}
