package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStopId
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import org.springframework.stereotype.Component

@Component
class JourneyMapper(
    private val lineVersionMapper: LineVersionMapper,
    private val timetableStopMapper: TimetableStopMapper,
    private val routeMapper: RouteMapper,
) {
    fun toDomain(journey: DbJourney): Journey = Journey(
        journeyId = JourneyId(journey.externalId),
        lineVersion = lineVersionMapper.toDomain(journey.lineVersion),
        journeyPatternId = JourneyPatternId(journey.journeyPatternId),
        schedule = journey.schedule.sortedBy { it.id.stopOrder }.map(::scheduledStopToDomain),
        operatingPeriods = journey.operatingPeriods.map(::operatingPeriodToDomain),
        route = journey.route?.let(routeMapper::toDomain)
    )

    private fun scheduledStopToDomain(scheduledStop: DbScheduledStop): ScheduledStop = ScheduledStop(
        timetableStop = timetableStopMapper.toDomain(scheduledStop.timetableStop),
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    private fun operatingPeriodToDomain(operatingPeriod: DbOperatingPeriod): OperatingPeriod = OperatingPeriod(
        timeZone = operatingPeriod.timezone,
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays
    )

    fun toDb(journey: Journey): DbJourney {
        val dbLineVersion = lineVersionMapper.toDb(journey.lineVersion)
        val schedule = mutableListOf<DbScheduledStop>()

        val dbJourney = DbJourney(
            relationalId = null,
            externalId = journey.journeyId.value,
            journeyPatternId = journey.journeyPatternId.value,
            lineVersion = dbLineVersion,
            route = journey.route?.let(routeMapper::toDb),
            schedule = schedule,
            operatingPeriods = journey.operatingPeriods.map(::operatingPeriodToDb),
        )
        schedule.addAll(journey.schedule.mapIndexed { index, scheduledStop -> scheduledStopToDb(scheduledStop, dbJourney, index) })

        return dbJourney
    }

    private fun scheduledStopToDb(
        scheduledStop: ScheduledStop,
        journey: DbJourney,
        order: Int,
    ): DbScheduledStop = DbScheduledStop(
        id = DbScheduledStopId(journey.relationalId, order),
        timetableStop = timetableStopMapper.toDb(scheduledStop.timetableStop),
        journey = journey,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    private fun operatingPeriodToDb(operatingPeriod: OperatingPeriod): DbOperatingPeriod = DbOperatingPeriod(
        relationalId = null,
        timezone = operatingPeriod.timeZone,
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays,
    )
}
