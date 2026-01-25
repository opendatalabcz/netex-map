package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Repository
interface LineVersionJpaRepository: JpaRepository<DbLineVersion, Long> {
    @Query("SELECT lv FROM DbLineVersion lv " +
            "WHERE lv.externalId = :lineExternalId AND " +
                "lv.validFrom = :validFrom AND " +
                "lv.validTo = :validTo AND " +
                "lv.timezone = :timezone"
    )
    fun findByLineIdAndValidRange(
        @Param("lineExternalId") lineExternalId: String,
        @Param("validFrom") validFrom: LocalDateTime,
        @Param("validTo") validTo: LocalDateTime,
        @Param("timezone") timezone: ZoneId,
    ): Optional<DbLineVersion>
}
