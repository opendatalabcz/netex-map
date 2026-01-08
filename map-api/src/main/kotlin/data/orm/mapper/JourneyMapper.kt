package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
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
    private val journeyJpaRepository: JourneyJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
) {
    fun toDomain(journey: DbJourney): Journey = Journey(
        journeyId = JourneyId(journey.externalId),
        lineVersion = lineVersionMapper.toDomain(journey.lineVersion),
        journeyPatternId = JourneyPatternId(journey.journeyPatternId),
        schedule = journey.schedule.map { scheduledStopToDomain(it) },
        operatingPeriod = operatingPeriodToDomain(journey.operatingPeriod),
        route = journey.route?.let(routeMapper::toDomain)
    )

    private fun scheduledStopToDomain(
        scheduledStop: DbScheduledStop,
    ): ScheduledStop = ScheduledStop(
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
        val saved = journeyJpaRepository.findByExternalIdAndLineIdAndValidRange(
            externalId = journey.journeyId.value,
            lineExternalId = journey.lineVersion.line.lineId.value,
            validFrom = journey.lineVersion.validIn.from,
            validTo = journey.lineVersion.validIn.to,
            timezone = journey.lineVersion.validIn.timezone,
        )
        val dbLineVersion = lineVersionMapper.toDb(journey.lineVersion)
        val schedule = mutableListOf<DbScheduledStop>()

        val dbJourney = DbJourney(
            relationalId = saved.map { it.relationalId }.orElse(null),
            externalId = journey.journeyId.value,
            journeyPatternId = journey.journeyPatternId.value,
            lineVersion = dbLineVersion,
            route = journey.route?.let(routeMapper::toDb),
            schedule = schedule,
            operatingPeriod = operatingPeriodToDb(journey.operatingPeriod, dbLineVersion),
        )
        schedule.addAll(journey.schedule.mapIndexed { index, scheduledStop -> scheduledStopToDb(scheduledStop, dbJourney, index) })

        return dbJourney
    }

    private fun scheduledStopToDb(scheduledStop: ScheduledStop, journey: DbJourney, order: Int): DbScheduledStop {
        return DbScheduledStop(
            id = DbScheduledStopId(journey.relationalId, order),
            timetableStop = timetableStopMapper.toDb(scheduledStop.timetableStop),
            journey = journey,
            arrival = scheduledStop.arrival,
            departure = scheduledStop.departure,
        )
    }

    private fun operatingPeriodToDb(operatingPeriod: OperatingPeriod, lineVersion: DbLineVersion): DbOperatingPeriod {
        val saved = operatingPeriodJpaRepository.findByLineVersionIdAndValidDays(
            lineExternalId = lineVersion.line.externalId,
            validFrom = lineVersion.validFrom,
            validTo = lineVersion.validTo,
            timezone = lineVersion.timezone,
            validDays = operatingPeriod.validDays,
        )
        return DbOperatingPeriod(
            relationalId = saved.map { it.relationalId }.orElse(null),
            timezone = operatingPeriod.timeZone,
            fromDate = operatingPeriod.fromDate,
            toDate = operatingPeriod.toDate,
            validDays = operatingPeriod.validDays,
        )
    }
}
