package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.LineVersionFrameDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails.JourneyDetailsLineVersionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.Optional

@Repository
interface LineVersionJpaRepository: JpaRepository<LineVersion, Long> {
    companion object {
        private const val FIND_ALL_PUBLIC_CODES_JPQL_QUERY = "SELECT DISTINCT lv.publicCode FROM LineVersion lv ORDER BY lv.publicCode"
    }
    @Query("""
        SELECT lv.relationalId
        FROM LineVersion lv
        WHERE lv.publicCode = :publicCode AND
            lv.validFrom = :validFrom AND
            lv.validTo = :validTo AND
            lv.isDetour = :isDetour
    """)
    fun findIdByPublicCodeAndValidRangeAndDetour(
        publicCode: String,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
        isDetour: Boolean,
    ): Optional<Long>

    @Query(FIND_ALL_PUBLIC_CODES_JPQL_QUERY)
    fun findAllPublicCodes(): List<String>

    @Query(FIND_ALL_PUBLIC_CODES_JPQL_QUERY)
    fun findAllPublicCodes(pageable: Pageable): Page<String>

    @Query("SELECT lv FROM LineVersion lv WHERE lv.publicCode IN :publicCodes")
    fun findAllByPublicCodes(publicCodes: List<String>): List<LineVersion>

    @Query(nativeQuery = true, value = """
        SELECT
            lv.relational_id,
            lv.public_code,
            lv.name,
            lv.short_name,
            lv.transport_mode,
            lv.line_type,
            lv.is_detour,
            lv.valid_from,
            lv.valid_to,
            lv.operator_id
        FROM line_version lv
        WHERE lv.relational_id = :id
    """)
    fun findDtoById(id: Long): Optional<LineVersionDto>

    @Query(nativeQuery = true, value = """
        SELECT
            lv.relational_id,
            lv.public_code,
            lv.name,
            lv.short_name,
            lv.transport_mode,
            lv.line_type,
            lv.is_detour,
            lv.operator_id
        FROM line_version lv
        WHERE lv.relational_id = :id
    """)
    fun findJourneyDetailsDtoById(id: Long): Optional<JourneyDetailsLineVersionDto>

    @Query(nativeQuery = true, value = """
        SELECT
            lv.relational_id,
            lv.public_code,
            lv.name,
            lv.short_name,
            lv.transport_mode,
            lv.line_type,
            lv.is_detour,
            lv.valid_from,
            lv.valid_to,
            lv.operator_id
        FROM line_version lv
            CROSS JOIN LATERAL ( SELECT CONCAT('%', :query, '%') as query_pattern )
            CROSS JOIN LATERAL ( SELECT
                lv.public_code ILIKE query_pattern ESCAPE '\' as public_code_match,
                lv.short_name ILIKE query_pattern ESCAPE '\' as short_name_match
            )
        WHERE public_code_match OR short_name_match
        ORDER BY LEAST(
            CASE WHEN public_code_match THEN LENGTH(lv.public_code) ELSE ${Integer.MAX_VALUE} END,
            CASE WHEN short_name_match THEN LENGTH(lv.short_name) ELSE ${Integer.MAX_VALUE} END
        ) ASC NULLS LAST
    """)
    fun searchDto(query: String, pageable: Pageable): Page<LineVersionDto>

    @Query(nativeQuery = true, value = """
        SELECT lv.relational_id, lv.line_type
        FROM line_version lv
        WHERE lv.relational_id IN :lineVersionIds
    """)
    fun findAllFrameDtoByLineVersionIds(lineVersionIds: List<Long>): List<LineVersionFrameDto>

    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM LineVersion lv 
            WHERE lv.publicCode = :publicCode 
            AND (lv.lineType = cz.cvut.fit.gaierda1.data.orm.model.LineType.INTERNATIONAL_EXCLUDING_CABOTAGE 
                 OR lv.lineType = cz.cvut.fit.gaierda1.data.orm.model.LineType.INTERNATIONAL_INCLUDING_CABOTAGE)
        )
    """)
    fun areLinesByPublicCodeInternational(publicCode: String): Boolean
}
