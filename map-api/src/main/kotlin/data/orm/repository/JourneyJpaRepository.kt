package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails.JourneyDetailsDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.JourneyByDistinctJourneyPatternDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.JourneyFrameDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyWallDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.Optional

@Repository
interface JourneyJpaRepository: JpaRepository<Journey, Long> {
    private companion object {
        const val OPERATING_IN_FRAME_QUERY = """
            SELECT j.relational_id, j.line_version_id, jp.route_id, j.next_day_first_stop_index, j.timezone
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
                JOIN journey_pattern jp on (jp.line_version_id = j.line_version_id AND jp.pattern_number = j.pattern_number)
                JOIN active_period ap ON jp.line_version_id = ap.line_version_id
                JOIN operating_period op ON j.operating_period_id = op.relational_id
                JOIN route r ON jp.route_id = r.relational_id
            WHERE jp.route_id IS NOT NULL
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
            SELECT j.relational_id, j.line_version_id, jp.route_id, j.next_day_first_stop_index, j.timezone
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
                JOIN journey_pattern jp on (jp.line_version_id = j.line_version_id AND jp.pattern_number = j.pattern_number)
                JOIN active_period ap ON jp.line_version_id = ap.line_version_id
                JOIN operating_period op ON j.operating_period_id = op.relational_id
                JOIN route r ON jp.route_id = r.relational_id
            WHERE jp.route_id IS NOT NULL
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
        SELECT j.relationalId FROM Journey j
        WHERE j.journeyNumber = :journeyNumber AND
            j.journeyPattern.lineVersion.publicCode = :publicCode AND
            j.journeyPattern.lineVersion.validFrom = :validFrom AND
            j.journeyPattern.lineVersion.validTo = :validTo AND
            j.journeyPattern.lineVersion.isDetour = :isDetour
    """)
    fun findIdByJourneyNumberAndLinePublicCodeAndValidRangeAndDetour(
        journeyNumber: String,
        publicCode: String,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
        isDetour: Boolean,
    ): Optional<Long>

    @Query(nativeQuery = true, value = OPERATING_IN_FRAME_QUERY)
    fun findAllFrameDtoOperatingInFrame(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        minRouteLength: Double,
        targetMoment: OffsetDateTime,
    ): List<JourneyFrameDto>

    @Query(nativeQuery = true, value = OPERATING_IN_FRAME_FOR_PREVIOUS_DAY_QUERY)
    fun findAllFrameDtoOperatingInFrameWithNextDayOperation(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        minRouteLength: Double,
        targetMoment: OffsetDateTime,
    ): List<JourneyFrameDto>

    @Query(nativeQuery = true, value = """
        SELECT DISTINCT ON (j.line_version_id, j.pattern_number) j.relational_id, j.line_version_id, j.pattern_number
        FROM journey j
            JOIN journey_pattern jp ON jp.line_version_id = j.line_version_id AND jp.pattern_number = j.pattern_number
        WHERE jp.route_id IS NULL
        ORDER BY j.line_version_id, j.pattern_number
    """, countQuery = """
        SELECT COUNT(DISTINCT (j.line_version_id, j.pattern_number))
        FROM journey j
            JOIN journey_pattern jp ON jp.line_version_id = j.line_version_id AND jp.pattern_number = j.pattern_number
        WHERE jp.route_id IS NULL
    """)
    fun findAllDistinctJourneyPatternDtoWithNullRoute(pageable: Pageable): Page<JourneyByDistinctJourneyPatternDto>

    @Query(nativeQuery = true, value = """
        SELECT
            j.relational_id,
            j.pattern_number,
            j.operating_period_id,
            j.requires_ordering,
            j.baggage_storage,
            j.cycles_allowed,
            j.low_floor_access,
            j.reservation_compulsory,
            j.reservation_possible,
            j.snacks_on_board,
            j.unaccompanied_minor_assistance
        FROM journey j
            LEFT OUTER JOIN journey_pattern jp ON jp.line_version_id = j.line_version_id AND jp.pattern_number = j.pattern_number
        WHERE j.line_version_id = :lineVersionId
    """)
    fun findAllWallDtoByLineVersionId(lineVersionId: Long): List<JourneyWallDto>

    @Query(nativeQuery = true, value = """
        SELECT
            j.line_version_id,
            j.pattern_number,
            jp.route_id,
            j.requires_ordering,
            j.baggage_storage,
            j.cycles_allowed,
            j.low_floor_access,
            j.reservation_compulsory,
            j.reservation_possible,
            j.snacks_on_board,
            j.unaccompanied_minor_assistance
        FROM journey j
            LEFT OUTER JOIN journey_pattern jp ON jp.line_version_id = j.line_version_id AND jp.pattern_number = j.pattern_number
        WHERE j.relational_id = :journeyId
    """)
    fun findDetailsDtoByJourneyId(journeyId: Long): Optional<JourneyDetailsDto>
}
