package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.misc.atOffset
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.map.JourneyMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.map.RouteMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.map.RouteStopMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.map.ScheduledStopMapDto
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInFrameUseCase.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class GetJourneysOperatingInFrame(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val levelOfDetailUseCase: LevelOfDetailUseCase,
): GetJourneysOperatingInFrameUseCase {
    private fun scheduledStopToMap(
        stopIndex: Int,
        scheduledStop: ScheduledStopMapDto,
        day: LocalDate,
        journeyTimezone: ZoneId,
        nextDayFirstStopIndex: Int?,
    ): MapScheduledStop {
        var departure: OffsetDateTime? = scheduledStop.departure?.let { LocalDateTime.of(day, it).atOffset(journeyTimezone) }
        var arrival: OffsetDateTime? = if (scheduledStop.departure == scheduledStop.arrival) null
            else scheduledStop.arrival?.let { LocalDateTime.of(day, it).atOffset(journeyTimezone) }

        if (nextDayFirstStopIndex != null) {
            if (stopIndex >= nextDayFirstStopIndex) {
                arrival = arrival?.plusDays(1)
                departure = departure?.plusDays(1)
            }
            else if (stopIndex == nextDayFirstStopIndex - 1
                && arrival != null && departure != null
                && arrival > departure
            ) {
                departure = departure.plusDays(1)
            }
        }

        return MapScheduledStop(
            arrival = arrival,
            departure = departure,
        )
    }

    private fun recomputeJourneysToMap(
        journeys: List<JourneyMapDto>,
        day: OffsetDateTime,
        scheduleStops: Map<Long, List<ScheduledStopMapDto>>,
        fromPreviousDay: Boolean,
    ): List<MapJourney> = journeys.map { journey -> MapJourney(
        relationalId = journey.relationalId,
        lineVersionId = journey.lineVersionId,
        routeId = journey.routeId,
        schedule = scheduleStops[journey.relationalId]!!.mapIndexed { idx, stop ->
            scheduledStopToMap(
                idx,
                stop,
                day.toLocalDate(),
                ZoneId.of(journey.timezone),
                journey.nextDayFirstStopIndex,
            )
        },
        nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
        fromPreviousDay = fromPreviousDay,
    ) }

    /**
     * Returns all journeys operating in a given day.
     *
     * Works correctly only for journeys that operate in the same timezone as parameter `timezone`.
     * Other journeys may not operate in the time-range of the specified day.
     */
    @Transactional(readOnly = true)
    override fun getJourneysOperatingInFrame(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        zoomLevel: Int,
        dateTime: OffsetDateTime,
    ): JourneysOperatingInFrameResult {
        val minRouteLength = levelOfDetailUseCase.getMinRouteLength(zoomLevel)

        val journeysForCurrentDay = journeyJpaRepository
            .findAllMapDtoOperatingInFrame(
                lonMin, latMin, lonMax, latMax, minRouteLength, dateTime
            )

        val journeysForPreviousDay = journeyJpaRepository
            .findAllMapDtoOperatingInFrameWithNextDayOperation(
                lonMin, latMin, lonMax, latMax, minRouteLength, dateTime
            )

        val lineVersions = lineVersionJpaRepository.findAllMapDtoByIds(
            journeysForCurrentDay.map(JourneyMapDto::lineVersionId)
                    + journeysForPreviousDay.map(JourneyMapDto::lineVersionId)
        )

        val scheduleStops = scheduledStopJpaRepository.findAllMapDtoByJourneyId(
            journeysForCurrentDay.map(JourneyMapDto::relationalId)
                    + journeysForPreviousDay.map(JourneyMapDto::relationalId)
        ).groupBy(ScheduledStopMapDto::journeyId)
            .mapValues { (_, schedule) -> schedule.sortedBy(ScheduledStopMapDto::stopOrder) }

        val recomputedForDay = recomputeJourneysToMap(journeysForCurrentDay, dateTime, scheduleStops, false)
        val recomputedForPreviousDay = recomputeJourneysToMap(journeysForPreviousDay, dateTime.minusDays(1), scheduleStops, true)

        val rawRoutes = routeJpaRepository.findAllMapDtoByRouteId(
            journeysForCurrentDay.map(JourneyMapDto::routeId)
                    + journeysForPreviousDay.map(JourneyMapDto::routeId)
        )
        val routeStops = routeStopJpaRepository.findAllDtoByRouteIds(rawRoutes.map(RouteMapDto::relationalId))
            .groupBy(RouteStopMapDto::routeId)
            .mapValues { (_, routeStops) -> routeStops
                .sortedBy(RouteStopMapDto::stopOrder)
                .map(RouteStopMapDto::routeFraction)
            }

        val recomputedRoutes = rawRoutes.map { route -> MapRoute(
            relationalId = route.relationalId,
            pointSequence = route.pointSequence,
            totalDistance = route.totalDistance,
            routeStops = routeStops[route.relationalId]!!,
        ) }

        return JourneysOperatingInFrameResult (
            journeys = recomputedForDay + recomputedForPreviousDay,
            routes = recomputedRoutes,
            lineVersions = lineVersions,
        )
    }
}
