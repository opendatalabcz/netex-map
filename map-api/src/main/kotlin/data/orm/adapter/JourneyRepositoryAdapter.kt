package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import org.springframework.stereotype.Component

@Component
class JourneyRepositoryAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val lineVersionRepositoryAdapter: LineVersionRepositoryAdapter,
    private val routeRepositoryAdapter: RouteRepositoryAdapter,
): JourneyRepository {
    fun toDomain(journey: DbJourney): Journey = Journey(
        journeyId = JourneyId(journey.externalId),
        lineVersion = lineVersionRepositoryAdapter.toDomain(journey.lineVersion),
        journeyPatternId = JourneyPatternId(journey.journeyPatternId),
        schedule = journey.schedule.sortedBy { it.stopId.stopOrder }.map(::toDomain),
        operatingPeriods = journey.operatingPeriods.map(::toDomain),
        route = journey.route?.let(routeRepositoryAdapter::toDomain)
    )

    fun toDomain(scheduledStop: DbScheduledStop): ScheduledStop = ScheduledStop(
        name = scheduledStop.name,
        stopOnRequest = scheduledStop.stopOnRequest,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    fun toDomain(operatingPeriod: DbOperatingPeriod): OperatingPeriod = OperatingPeriod(
        timezone = operatingPeriod.timezone,
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays
    )

    fun toDb(journey: Journey, relationalId: Long?): DbJourney {
        val schedule = mutableListOf<DbScheduledStop>()
        val dbJourney = DbJourney(
            relationalId = relationalId,
            externalId = journey.journeyId.value,
            journeyPatternId = journey.journeyPatternId.value,
            lineVersion = lineVersionRepositoryAdapter.findSaveMapping(journey.lineVersion),
            route = journey.route?.let(routeRepositoryAdapter::findSaveMapping),
            schedule = schedule,
            operatingPeriods = journey.operatingPeriods.map(::findSaveMapping),
        )
        schedule.addAll(journey.schedule.mapIndexed { index, scheduledStop -> toDb(scheduledStop, dbJourney, index) })
        return dbJourney
    }

    fun toDb(
        scheduledStop: ScheduledStop,
        journey: DbJourney,
        order: Int,
    ): DbScheduledStop = DbScheduledStop(
        stopId = DbScheduledStopId(journey.relationalId, order),
        name = scheduledStop.name,
        stopOnRequest = scheduledStop.stopOnRequest,
        journey = journey,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    fun toDb(operatingPeriod: OperatingPeriod, relationalId: Long?): DbOperatingPeriod = DbOperatingPeriod(
        relationalId = relationalId,
        timezone = operatingPeriod.timezone,
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays,
    )

    fun findSaveMapping(operatingPeriod: OperatingPeriod): DbOperatingPeriod {
        val optionalSaved = operatingPeriodJpaRepository.findByLineVersionIdAndValidDays(
            fromDate = operatingPeriod.fromDate,
            toDate = operatingPeriod.toDate,
            timezone = operatingPeriod.timezone,
            validDays = operatingPeriod.validDays,
        )
        return optionalSaved.orElseGet { operatingPeriodJpaRepository.save(toDb(operatingPeriod, null)) }
    }

    fun findSaveMapping(journey: Journey): DbJourney {
        val optionalSaved = journeyJpaRepository.findByExternalIdAndLineIdAndValidRange(
            externalId = journey.journeyId.value,
            lineExternalId = journey.lineVersion.lineId.value,
            validFrom = journey.lineVersion.validIn.from,
            validTo = journey.lineVersion.validIn.to,
            timezone = journey.lineVersion.validIn.timezone,
            isDetour = journey.lineVersion.isDetour,
        )
        if (optionalSaved.isPresent) {
            return optionalSaved.get()
        }
        val saved = journeyJpaRepository.save(toDb(
            journey = journey,
            relationalId = null
        ))
        for (scheduledStop in saved.schedule) {
            scheduledStop.stopId.journeyId = saved.relationalId
        }
        scheduledStopJpaRepository.saveAll(saved.schedule)
        return saved
    }

    override fun save(journey: Journey) {
        findSaveMapping(journey)
    }

    override fun findById(lineId: LineId, validRange: DateRange, isDetour: Boolean, journeyId: JourneyId): Journey? {
        return journeyJpaRepository
            .findByExternalIdAndLineIdAndValidRange(
                externalId = journeyId.value,
                lineExternalId = lineId.value,
                validFrom = validRange.from,
                validTo = validRange.to,
                timezone = validRange.timezone,
                isDetour = isDetour,
            ).map(::toDomain)
            .orElse(null)
    }
}
