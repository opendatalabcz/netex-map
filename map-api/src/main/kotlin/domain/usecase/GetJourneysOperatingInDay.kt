package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInDayUseCase.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class GetJourneysOperatingInDay(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
): GetJourneysOperatingInDayUseCase {
    private fun scheduledStopToDaySpecific(
        stopIndex: Int,
        scheduledStop: ScheduledStop,
        day: LocalDate,
        zoneId: ZoneId,
        nextDayFirstStopIndex: Int?,
    ): DaySpecificScheduledStop {
        var arrival: ZonedDateTime? = scheduledStop.arrival?.let { ZonedDateTime.of(day, it, zoneId) }
        var departure: ZonedDateTime? = scheduledStop.departure?.let { ZonedDateTime.of(day, it, zoneId) }

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

        return DaySpecificScheduledStop(
            name = scheduledStop.name,
            stopOnRequest = scheduledStop.stopOnRequest,
            arrival = arrival,
            departure = departure,
        )
    }

    private fun recomputeJourneysToSpecificDay(journeys: List<Journey>, day: LocalDate): List<DaySpecificJourney> {
        return journeys.map { journey ->
            val journeyTimezone = journey.operatingPeriods.first().timezone
            DaySpecificJourney(
                relationalId = journey.relationalId!!,
                lineVersion = journey.lineVersion,
                routeId = journey.route?.relationalId,
                schedule = journey.schedule.sortedBy { it.stopId.stopOrder }.mapIndexed { idx, stop ->
                    scheduledStopToDaySpecific(idx, stop, day, journeyTimezone, journey.nextDayFirstStopIndex)
                },
                nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
            )
        }
    }

    /**
     * Returns all journeys operating in a given day.
     *
     * Works correctly only for journeys that operate in the same timezone as parameter `timezone`.
     * Other journeys may not operate in the time-range of the specified day.
     */
    @Transactional(readOnly = true)
    override fun getJourneysOperatingInDay(day: LocalDate, timezone: ZoneId): JourneysOperatingInDayResult {
        val journeysForDay = journeyJpaRepository
            .findAllOperatingInRange(ZonedDateTime.of(day, LocalTime.MIN, timezone), ZonedDateTime.of(day, LocalTime.MAX, timezone))
        val journeysForPreviousDay = journeyJpaRepository
            .findAllOperatingInRangeWithNextDayOperation(ZonedDateTime.of(day.minusDays(1), LocalTime.MIN, timezone), ZonedDateTime.of(day.minusDays(1), LocalTime.MAX, timezone))

        val recomputedForDay = recomputeJourneysToSpecificDay(journeysForDay, day)
        val recomputedForPreviousDay = recomputeJourneysToSpecificDay(journeysForPreviousDay, day.minusDays(1))
        val routes = routeJpaRepository.findAllByJourney(recomputedForDay.map { it.relationalId } + recomputedForPreviousDay.map { it.relationalId })

        return JourneysOperatingInDayResult (
            startingThisDay = recomputedForDay,
            continuingThisDay = recomputedForPreviousDay,
            routes = routes,
        )
    }
}