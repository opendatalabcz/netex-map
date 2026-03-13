package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.Route
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional

@Repository
interface JourneyJpaRepository: JpaRepository<Journey, Long> {
    private companion object {
        const val OPERATING_IN_RANGE_QUERY = "SELECT DISTINCT j.* " +
            "FROM journey j " +
                "JOIN line_version lv ON j.line_version_id = lv.relational_id " +
                "JOIN journey_operating_period jop ON j.relational_id = jop.journey_id " +
                "JOIN operating_period op ON jop.operating_period_id = op.relational_id " +
            "WHERE (TIMEZONE(lv.timezone, lv.active_from) <= :from AND TIMEZONE(lv.timezone, lv.active_to) > :from OR " +
                "TIMEZONE(lv.timezone, lv.active_from) < :to AND TIMEZONE(lv.timezone, lv.active_to) >= :to) AND " +
                "(op.valid_days[DATE_PART('day', :from - TIMEZONE(op.timezone, op.from_date)) + 1] = true OR " +
                "op.valid_days[DATE_PART('day', :to - TIMEZONE(op.timezone, op.from_date)) + 1] = true)"
    }

    @Query("SELECT j FROM Journey j " +
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
    ): Optional<Journey>

    @Query("SELECT j FROM Journey j WHERE j.route IS NULL")
    fun findByNullRoute(pageable: Pageable): Page<Journey>

    @Query(nativeQuery = true, value = OPERATING_IN_RANGE_QUERY)
    fun findAllOperatingInRange(from: ZonedDateTime, to: ZonedDateTime): List<Journey>

    @Query(nativeQuery = true, value = "$OPERATING_IN_RANGE_QUERY AND j.next_day_first_stop_index IS NOT NULL")
    fun findAllOperatingInRangeWithNextDayOperation(from: ZonedDateTime, to: ZonedDateTime): List<Journey>

    @Query("SELECT j FROM Journey j JOIN FETCH j.route")
    fun findAllFetchRoutes(pageable: Pageable): Page<Journey>

    @Query(nativeQuery = true, value = "SELECT DISTINCT ON (line_version_id, journey_pattern_id) * FROM journey " +
            "WHERE route_id IS NULL " +
            "ORDER BY line_version_id, journey_pattern_id")
    fun findAllWithDistinctJourneyPatternWithNullRoute(pageable: Pageable): Page<Journey>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Journey j SET j.route = :route " +
            "WHERE j.lineVersion = :lineVersion AND j.journeyPatternId = :journeyPatternId")
    fun setRouteForAllByLineVersionAndJourneyPattern(lineVersion: LineVersion, journeyPatternId: String, route: Route)
}
