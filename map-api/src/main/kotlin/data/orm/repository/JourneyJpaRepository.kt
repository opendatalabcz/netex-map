package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.repository.dto.JourneyMapDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.Optional

@Repository
interface JourneyJpaRepository: JpaRepository<Journey, Long> {
    private companion object {
        const val OPERATING_IN_FRAME_QUERY = """
            SELECT DISTINCT j.relational_id, j.line_version_id, j.route_id, j.next_day_first_stop_index, j.timezone
            FROM journey j
                JOIN line_version lv ON j.line_version_id = lv.relational_id
                JOIN operating_period op ON j.operating_period_id = op.relational_id
                JOIN route r ON j.route_id = r.relational_id
            WHERE j.route_id IS NOT NULL
                AND :targetMoment BETWEEN lv.active_from AND lv.active_to
                AND op.valid_days[ 1 +
                    ((:targetMoment AT TIME ZONE j.timezone)::date - (op.from_date AT TIME ZONE j.timezone)::date)
                ] AND r.point_sequence && ST_MakeEnvelope(:lonMin, :latMin, :lonMax, :latMax, 4326)
                AND r.total_distance >= :minRouteLength
        """
    }

    @Query("""
        SELECT j FROM Journey j
        WHERE j.externalId = :externalId AND
            j.lineVersion.externalId = :lineExternalId AND
            j.lineVersion.validFrom = :validFrom AND
            j.lineVersion.validTo = :validTo AND
            j.lineVersion.isDetour = :isDetour
    """)
    fun findByExternalIdAndLineIdAndValidRange(
        externalId: String,
        lineExternalId: String,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
        isDetour: Boolean,
    ): Optional<Journey>

    @Query(nativeQuery = true, value = OPERATING_IN_FRAME_QUERY)
    fun findAllMapDtoOperatingInFrame(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        minRouteLength: Double,
        targetMoment: OffsetDateTime,
    ): List<JourneyMapDto>

    @Query(nativeQuery = true, value = "$OPERATING_IN_FRAME_QUERY AND j.next_day_first_stop_index IS NOT NULL")
    fun findAllMapDtoOperatingInFrameWithNextDayOperation(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        minRouteLength: Double,
        targetMoment: OffsetDateTime,
    ): List<JourneyMapDto>

    @Query("SELECT j FROM Journey j JOIN FETCH j.route")
    fun findAllFetchRoutes(pageable: Pageable): Page<Journey>

    @Query(nativeQuery = true, value = """
        SELECT DISTINCT ON (line_version_id, journey_pattern_id) *
        FROM journey
        WHERE route_id IS NULL
        ORDER BY line_version_id, journey_pattern_id
    """, countQuery = "SELECT COUNT(DISTINCT (line_version_id, journey_pattern_id)) FROM journey WHERE route_id IS NULL")
    fun findAllWithDistinctJourneyPatternWithNullRoute(pageable: Pageable): Page<Journey>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Journey j SET j.route = :route
        WHERE j.lineVersion = :lineVersion AND j.journeyPatternId = :journeyPatternId
    """)
    fun setRouteForAllByLineVersionAndJourneyPattern(lineVersion: LineVersion, journeyPatternId: String, route: Route)
}
