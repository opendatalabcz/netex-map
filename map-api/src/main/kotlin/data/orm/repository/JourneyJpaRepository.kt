package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.repository.dto.map.JourneyMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyWallDto
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
            SELECT j.relational_id, j.line_version_id, j.route_id, j.next_day_first_stop_index, j.timezone
            FROM journey j
                CROSS JOIN LATERAL ( SELECT
                    (:targetMoment)::timestamptz AT TIME ZONE j.timezone as local_target_moment
                )
                CROSS JOIN LATERAL ( SELECT
                    (local_target_moment::date + j.begin_time) AT TIME ZONE j.timezone as journey_start_moment,
                    CASE WHEN j.next_day_first_stop_index IS NULL THEN
                        (local_target_moment::date + j.end_time) AT TIME ZONE j.timezone
                    ELSE
                        (local_target_moment::date + interval '24 hours' + j.end_time) AT TIME ZONE j.timezone
                    END as journey_end_moment,
                    date_trunc('hour', local_target_moment) as target_start_moment,
                    date_trunc('hour', local_target_moment) + interval '1 hour' as target_end_moment
                )
                JOIN line_version lv ON j.line_version_id = lv.relational_id
                JOIN active_period ap ON lv.relational_id = ap.line_version_id
                JOIN operating_period op ON j.operating_period_id = op.relational_id
                JOIN route r ON j.route_id = r.relational_id
            WHERE j.route_id IS NOT NULL
                AND journey_start_moment < target_end_moment
                AND journey_end_moment > target_start_moment
                AND r.point_sequence && ST_MakeEnvelope(:lonMin, :latMin, :lonMax, :latMax, 4326)
                AND r.total_distance >= :minRouteLength
                AND (:targetMoment)::timestamptz BETWEEN ap.from_date AND ap.to_date
                AND op.valid_days[ 1 +
                    (local_target_moment::date - (op.from_date AT TIME ZONE j.timezone)::date)
                ]
        """
        const val OPERATING_IN_FRAME_FOR_PREVIOUS_DAY_QUERY = """
            SELECT j.relational_id, j.line_version_id, j.route_id, j.next_day_first_stop_index, j.timezone
            FROM journey j
                CROSS JOIN LATERAL ( SELECT
                    (:targetMoment)::timestamptz AT TIME ZONE j.timezone as local_target_moment,
                    ((:targetMoment)::timestamptz AT TIME ZONE j.timezone) - interval '1 day' as local_previous_day_moment
                )
                CROSS JOIN LATERAL ( SELECT
                    (local_previous_day_moment::date + j.begin_time) AT TIME ZONE j.timezone as journey_start_moment,
                    CASE WHEN j.next_day_first_stop_index IS NULL THEN
                        (local_previous_day_moment::date + j.end_time) AT TIME ZONE j.timezone
                    ELSE
                        (local_previous_day_moment::date + interval '24 hours' + j.end_time) AT TIME ZONE j.timezone
                    END as journey_end_moment,
                    date_trunc('hour', local_target_moment) as target_start_moment,
                    date_trunc('hour', local_target_moment) + interval '1 hour' as target_end_moment
                )
                JOIN line_version lv ON j.line_version_id = lv.relational_id
                JOIN active_period ap ON lv.relational_id = ap.line_version_id
                JOIN operating_period op ON j.operating_period_id = op.relational_id
                JOIN route r ON j.route_id = r.relational_id
            WHERE j.route_id IS NOT NULL
                AND j.next_day_first_stop_index IS NOT NULL
                AND journey_start_moment < target_end_moment
                AND journey_end_moment > target_start_moment
                AND r.point_sequence && ST_MakeEnvelope(:lonMin, :latMin, :lonMax, :latMax, 4326)
                AND r.total_distance >= :minRouteLength
                AND ((:targetMoment)::timestamptz - interval '1 day') BETWEEN ap.from_date AND ap.to_date
                AND op.valid_days[ 1 +
                    (local_previous_day_moment::date - (op.from_date AT TIME ZONE j.timezone)::date)
                ]
        """
    }

    @Query("""
        SELECT j FROM Journey j
        WHERE j.externalId = :externalId AND
            j.lineVersion.publicCode = :publicCode AND
            j.lineVersion.validFrom = :validFrom AND
            j.lineVersion.validTo = :validTo AND
            j.lineVersion.isDetour = :isDetour
    """)
    fun findByExternalIdAndLineIdAndValidRange(
        externalId: String,
        publicCode: String,
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

    @Query(nativeQuery = true, value = OPERATING_IN_FRAME_FOR_PREVIOUS_DAY_QUERY)
    fun findAllMapDtoOperatingInFrameWithNextDayOperation(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        minRouteLength: Double,
        targetMoment: OffsetDateTime,
    ): List<JourneyMapDto>

    @Query("SELECT j FROM Journey j JOIN FETCH j.route WHERE j.relationalId = :journeyId")
    fun findByIdFetchRoute(journeyId: Long): Optional<Journey>

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

    @Query(nativeQuery = true, value = """
        SELECT j.relational_id, j.operating_period_id
        FROM journey j
        WHERE j.line_version_id = :lineVersionId
    """)
    fun findAllWallDtoByLineVersionId(lineVersionId: Long): List<JourneyWallDto>
}
