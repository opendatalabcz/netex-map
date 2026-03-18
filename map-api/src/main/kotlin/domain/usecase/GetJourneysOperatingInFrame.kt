package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.JourneyMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.RouteMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.RouteStopMapDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopMapDto
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInFrameUseCase.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class GetJourneysOperatingInFrame(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
): GetJourneysOperatingInFrameUseCase {
    private fun scheduledStopToDaySpecific(
        stopIndex: Int,
        scheduledStop: ScheduledStopMapDto,
        day: LocalDate,
        zoneId: ZoneId,
        nextDayFirstStopIndex: Int?,
    ): MapScheduledStop {
        var departure: ZonedDateTime? = scheduledStop.departure?.let { ZonedDateTime.of(day, it, zoneId) }
        var arrival: ZonedDateTime? = if (scheduledStop.departure == scheduledStop.arrival) null
            else scheduledStop.arrival?.let { ZonedDateTime.of(day, it, zoneId) }

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

    private fun recomputeJourneysToSpecificDay(
        journeys: List<JourneyMapDto>,
        day: LocalDate,
        scheduleStops: Map<Long, List<ScheduledStopMapDto>>,
    ): List<MapJourney> = journeys.map { journey -> MapJourney(
        relationalId = journey.relationalId,
        lineVersionId = journey.lineVersionId,
        routeId = journey.routeId,
        schedule = scheduleStops[journey.relationalId]!!.mapIndexed { idx, stop ->
            scheduledStopToDaySpecific(
                idx,
                stop,
                day,
                ZoneId.of(journey.timezone),
                journey.nextDayFirstStopIndex,
            )
        },
        nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
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
        day: LocalDate,
        timezone: ZoneId,
    ): JourneysOperatingInFrameResult {
        val journeysForCurrentDay = journeyJpaRepository
            .findAllMapDtoOperatingInFrame(
                lonMin, latMin, lonMax, latMax,
                ZonedDateTime.of(day, LocalTime.MIN, timezone),
                ZonedDateTime.of(day, LocalTime.MAX, timezone)
            )

        val journeysForPreviousDay = journeyJpaRepository
            .findAllMapDtoOperatingInFrameWithNextDayOperation(
                lonMin, latMin, lonMax, latMax,
                ZonedDateTime.of(day.minusDays(1), LocalTime.MIN, timezone),
                ZonedDateTime.of(day.minusDays(1), LocalTime.MAX, timezone)
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

        val recomputedForDay = recomputeJourneysToSpecificDay(journeysForCurrentDay, day, scheduleStops)
        val recomputedForPreviousDay = recomputeJourneysToSpecificDay(journeysForPreviousDay, day.minusDays(1), scheduleStops)

        val rawRoutes = routeJpaRepository.findAllMapDtoByRouteId(
            journeysForCurrentDay.map(JourneyMapDto::routeId)
                    + journeysForPreviousDay.map(JourneyMapDto::routeId)
        )
        val routeStops = routeStopJpaRepository.findAllDtoByRouteIds(rawRoutes.map(RouteMapDto::relationalId))
            .groupBy(RouteStopMapDto::routeId)
            .mapValues { (_, routeStops) -> routeStops.sortedBy(RouteStopMapDto::stopOrder) }

        return JourneysOperatingInFrameResult (
            startingThisDay = recomputedForDay,
            continuingThisDay = recomputedForPreviousDay,
            routes = rawRoutes.map { route -> MapRoute(
                relationalId = route.relationalId,
                pointSequence = route.pointSequence,
                totalDistance = route.totalDistance,
                routeStops = routeStops[route.relationalId]!!.map { stop -> MapRouteStop(
                    pointSequenceIndex = stop.pointSequenceIndex,
                    distanceToNextStop = stop.distanceToNextStop,
                ) },
            ) },
            lineVersions = lineVersions,
        )
    }
}
