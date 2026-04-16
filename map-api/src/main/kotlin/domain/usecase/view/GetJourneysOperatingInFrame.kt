package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.domain.misc.atOffsetByZoneId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.JourneyFrameDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class GetJourneysOperatingInFrame(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val levelOfDetailUseCase: LevelOfDetailUseCase,
    private val getEncodedRoutesUseCase: GetEncodedRoutesUseCase,
): GetJourneysOperatingInFrameUseCase {
    private fun scheduledStopToFrame(
        stopIndex: Int,
        scheduledStop: ScheduledStopDto,
        day: LocalDate,
        journeyTimezone: ZoneId,
        nextDayFirstStopIndex: Int?,
    ): GetJourneysOperatingInFrameUseCase.MapScheduledStop {
        var departure: OffsetDateTime? = scheduledStop.departure?.let { LocalDateTime.of(day, it).atOffsetByZoneId(journeyTimezone) }
        var arrival: OffsetDateTime? = if (scheduledStop.departure == scheduledStop.arrival) null
            else scheduledStop.arrival?.let { LocalDateTime.of(day, it).atOffsetByZoneId(journeyTimezone) }

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

        return GetJourneysOperatingInFrameUseCase.MapScheduledStop(
            arrival = arrival,
            departure = departure,
        )
    }

    private fun recomputeJourneysToFrame(
        journeys: List<JourneyFrameDto>,
        day: OffsetDateTime,
        scheduleStops: Map<Long, List<ScheduledStopDto>>,
        fromPreviousDay: Boolean,
    ): List<GetJourneysOperatingInFrameUseCase.MapJourney> = journeys.map { journey ->
        GetJourneysOperatingInFrameUseCase.MapJourney(
            relationalId = journey.relationalId,
            lineVersionId = journey.lineVersionId,
            routeId = journey.routeId,
            schedule = scheduleStops[journey.relationalId]!!.mapIndexed { idx, stop ->
                scheduledStopToFrame(
                    idx,
                    stop,
                    day.toLocalDate(),
                    ZoneId.of(journey.timezone),
                    journey.nextDayFirstStopIndex,
                )
            },
            nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
            fromPreviousDay = fromPreviousDay,
        )
    }

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
        excludedJourneyIds: Set<Long>,
        excludedJourneyIdsFromPreviousDay: Set<Long>,
        excludedRouteIds: Set<Long>,
    ): GetJourneysOperatingInFrameUseCase.JourneysOperatingInFrameResult {
        val minRouteLength = levelOfDetailUseCase.getMinRouteLength(zoomLevel)

        val journeysForCurrentDay = journeyJpaRepository
            .findAllFrameDtoOperatingInFrame(
                lonMin, latMin, lonMax, latMax, minRouteLength, dateTime
            ).filter { it.relationalId !in excludedJourneyIds }

        val journeysForPreviousDay = journeyJpaRepository
            .findAllFrameDtoOperatingInFrameWithNextDayOperation(
                lonMin, latMin, lonMax, latMax, minRouteLength, dateTime
            ).filter { it.relationalId !in excludedJourneyIdsFromPreviousDay }

        val scheduleStops = scheduledStopJpaRepository.findAllDtoByJourneyIds(
            journeysForCurrentDay.map(JourneyFrameDto::relationalId)
                    + journeysForPreviousDay.map(JourneyFrameDto::relationalId)
        ).groupBy(ScheduledStopDto::journeyId)
            .mapValues { (_, schedule) -> schedule.sortedBy(ScheduledStopDto::stopOrder) }

        val recomputedForDay = recomputeJourneysToFrame(journeysForCurrentDay, dateTime, scheduleStops, false)
        val recomputedForPreviousDay = recomputeJourneysToFrame(journeysForPreviousDay, dateTime.minusDays(1), scheduleStops, true)

        val routeIds = (journeysForCurrentDay.map(JourneyFrameDto::routeId)
                + journeysForPreviousDay.map(JourneyFrameDto::routeId)
            ).filter { it !in excludedRouteIds }

        return GetJourneysOperatingInFrameUseCase.JourneysOperatingInFrameResult(
            journeys = recomputedForDay + recomputedForPreviousDay,
            routes = getEncodedRoutesUseCase.getEncodedRoutes(routeIds),
        )
    }
}
