package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionMapDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@Repository
interface LineVersionJpaRepository: JpaRepository<LineVersion, Long> {
    @Query("""
        SELECT lv FROM LineVersion lv
        WHERE lv.externalId = :lineExternalId AND
            lv.validFrom = :validFrom AND
            lv.validTo = :validTo AND
            lv.timezone = :timezone AND
            lv.isDetour = :isDetour
    """)
    fun findByLineIdAndValidRange(
        lineExternalId: String,
        validFrom: LocalDateTime,
        validTo: LocalDateTime,
        timezone: ZoneId,
        isDetour: Boolean,
    ): Optional<LineVersion>

    @Query("SELECT DISTINCT lv.publicCode FROM LineVersion lv ORDER BY lv.publicCode")
    fun findAllPublicCodes(pageable: Pageable): Page<String>

    @Query("SELECT lv FROM LineVersion lv WHERE lv.publicCode IN :publicCodes")
    fun findAllByPublicCodes(publicCodes: List<String>): List<LineVersion>

    @Query(nativeQuery = true, value = """
        SELECT lv.relational_id, lv.public_code, lv.transport_mode
        FROM line_version lv
        WHERE lv.relational_id IN :ids
    """)
    fun findAllMapDtoByIds(ids: List<Long>): List<LineVersionMapDto>
}
