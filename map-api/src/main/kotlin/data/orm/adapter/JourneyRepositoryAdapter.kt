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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class JourneyRepositoryAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val lineVersionRepositoryAdapter: LineVersionRepositoryAdapter,
    private val routeRepositoryAdapter: RouteRepositoryAdapter,
): JourneyRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toDomain(journey: DbJourney): Journey = Journey(
        journeyId = JourneyId(journey.externalId),
        lineVersion = lineVersionRepositoryAdapter.toDomain(journey.lineVersion),
        journeyPatternId = JourneyPatternId(journey.journeyPatternId),
        schedule = journey.schedule.sortedBy { it.id.stopOrder }.map(::toDomain),
        operatingPeriods = journey.operatingPeriods.map(::toDomain),
        route = journey.route?.let(routeRepositoryAdapter::toDomain)
    )

    fun toDomain(scheduledStop: DbScheduledStop): ScheduledStop = ScheduledStop(
        name = scheduledStop.name,
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
        id = DbScheduledStopId(journey.relationalId, order),
        name = scheduledStop.name,
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
            lineExternalId = journey.lineVersion.line.lineId.value,
            validFrom = journey.lineVersion.validIn.from,
            validTo = journey.lineVersion.validIn.to,
            timezone = journey.lineVersion.validIn.timezone,
        )
        if (optionalSaved.isPresent) {
            return findSaveExistIngMapping(journey, optionalSaved.get())
        }
        val saved = journeyJpaRepository.save(toDb(
            journey = journey,
            relationalId = null
        ))
        for (scheduledStop in saved.schedule) {
            scheduledStop.id.journeyId = saved.relationalId
        }
        scheduledStopJpaRepository.saveAll(saved.schedule)
        return saved
    }

    private fun findSaveExistIngMapping(journey: Journey, savedJourney: DbJourney): DbJourney {
        val journeyPatternDiffer = journey.journeyPatternId.value != savedJourney.journeyPatternId
        if (journeyPatternDiffer) {
            logDifference("journey pattern id", savedJourney.journeyPatternId, journey.journeyPatternId.value, savedJourney)
        }
        val stopsDifferResult = compareScheduledStopsDiffer(journey, savedJourney)
        val operatingPeriodsDiffer = compareOperatingPeriodsDiffer(journey, savedJourney)
        if (!journeyPatternDiffer && !stopsDifferResult.first && !operatingPeriodsDiffer) return savedJourney

        scheduledStopJpaRepository.deleteAll(stopsDifferResult.second)
        val mapped = journeyJpaRepository.save(toDb(journey, savedJourney.relationalId))
        scheduledStopJpaRepository.saveAll(mapped.schedule)
        return mapped
    }
    
    private fun compareScheduledStopsDiffer(journey: Journey, savedJourney: DbJourney): Pair<Boolean, List<DbScheduledStop>> {
        var stopsDiffer = false
        val toDelete = mutableListOf<DbScheduledStop>()
        val sortedSavedStops = savedJourney.schedule.sortedBy { it.id.stopOrder }
        if (journey.schedule.size != sortedSavedStops.size) logDifference("number of scheduled stops", sortedSavedStops.size, journey.schedule.size, savedJourney)
        for ((new, old) in journey.schedule.zipWithFill(sortedSavedStops)) {
            if (new != null && old != null) {
                if (new.name != old.name) {
                    stopsDiffer = true
                    logDifference("name", old.name, new.name, savedJourney, old)
                }
                if (new.arrival?.toNanoOfDay() != old.arrival?.toNanoOfDay()) {
                    stopsDiffer = true
                    logDifference("arrival time", old.arrival, new.arrival, savedJourney, old)
                }
                if (new.departure?.toNanoOfDay() != old.departure?.toNanoOfDay()) {
                    stopsDiffer = true
                    logDifference("departure time", old.departure, new.departure, savedJourney, old)
                }
            } else if (new != null) {
                stopsDiffer = true
            } else {
                stopsDiffer = true
                toDelete.add(old!!)
            }
        }
        return stopsDiffer to toDelete
    }

    private fun <T> operatingPeriodComparator(
        fromDate: (T) -> LocalDateTime,
        toDate: (T) -> LocalDateTime,
        timezone: (T) -> ZoneId,
        validDays: (T) -> List<Boolean>
    ): (a: T, b: T) -> Int { return { a: T, b: T ->
        var res = fromDate(a).compareTo(fromDate(b))
        if (res == 0) res = toDate(a).compareTo(toDate(b))
        if (res == 0) res = timezone(a).id.compareTo(timezone(b).id)
        if (res == 0) {
            val aDays = validDays(a).map { if (it) 1 else 0 }.joinToString("")
            val bDays = validDays(b).map { if (it) 1 else 0 }.joinToString("")
            res = aDays.compareTo(bDays)
        }
        res
    } }

    private fun compareOperatingPeriodsDiffer(journey: Journey, savedJourney: DbJourney): Boolean {
        val sortedNew = journey.operatingPeriods.sortedWith(operatingPeriodComparator(OperatingPeriod::fromDate, OperatingPeriod::toDate, OperatingPeriod::timezone, OperatingPeriod::validDays))
        val sortedOld = savedJourney.operatingPeriods.sortedWith(operatingPeriodComparator(DbOperatingPeriod::fromDate, DbOperatingPeriod::toDate, DbOperatingPeriod::timezone, DbOperatingPeriod::validDays))
        if (sortedNew.size != sortedOld.size) {
            logDifference("number of scheduled stops", sortedOld.size, sortedNew.size, savedJourney)
            return true
        }
        var operatingPeriodsDiffer = false
        for ((new, old) in sortedNew.zip(sortedOld)) {
            val newFromDate = ZonedDateTime.of(new.fromDate, new.timezone)
            val oldFromDate = ZonedDateTime.of(old.fromDate, old.timezone)
            if (!newFromDate.equals(oldFromDate)) {
                logDifference("operating period: from date", oldFromDate, newFromDate, savedJourney)
                operatingPeriodsDiffer = true
            }
            val newToDate = ZonedDateTime.of(new.toDate, new.timezone)
            val oldToDate = ZonedDateTime.of(old.toDate, old.timezone)
            if (!newToDate.equals(oldToDate)) {
                logDifference("operating period: to date", oldToDate, newToDate, savedJourney)
                operatingPeriodsDiffer = true
            }
            if (new.validDays != old.validDays) {
                logDifference("operating period: valid days", old.validDays, new.validDays, savedJourney)
                operatingPeriodsDiffer = true
            }
        }
        return operatingPeriodsDiffer
    }

    override fun save(journey: Journey) {
        findSaveMapping(journey)
    }

    override fun findById(lineId: LineId, validRange: DateRange, journeyId: JourneyId): Journey? {
        return journeyJpaRepository
            .findByExternalIdAndLineIdAndValidRange(
                externalId = journeyId.value,
                lineExternalId = lineId.value,
                validFrom = validRange.from,
                validTo = validRange.to,
                timezone = validRange.timezone,
            ).map(::toDomain)
            .orElse(null)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, contextJourney: DbJourney, contextStop: DbScheduledStop) {
        logDifference("stop [${contextStop.id.stopOrder}]: $fieldName", old, new, contextJourney)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbJourney) {
        log.warn(
            "Journey {} for line version {} {}-{}({}): {} changed from {} to {}",
            context.externalId,
            context.lineVersion.line.externalId,
            context.lineVersion.validFrom,
            context.lineVersion.validTo,
            context.lineVersion.timezone,
            fieldName,
            old,
            new
        )
    }
}
