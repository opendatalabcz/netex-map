package cz.cvut.fit.gaierda1.data.orm.adapter.pedantic

import cz.cvut.fit.gaierda1.data.orm.adapter.JourneyRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.adapter.LineVersionRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.adapter.OperatingPeriodRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.adapter.RouteRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.util.PageAdapter
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

class JourneyRepositoryPedanticAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    scheduledStopJpaRepository: ScheduledStopJpaRepository,
    operatingPeriodRepositoryAdapter: OperatingPeriodRepositoryAdapter,
    lineVersionRepositoryAdapter: LineVersionRepositoryAdapter,
    routeRepositoryAdapter: RouteRepositoryAdapter,
    pageAdapter: PageAdapter,
): JourneyRepositoryAdapter(journeyJpaRepository, scheduledStopJpaRepository, operatingPeriodRepositoryAdapter, lineVersionRepositoryAdapter, routeRepositoryAdapter, pageAdapter) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun findOrMap(
        journey: Journey,
        dependenciesSupplier: () -> Triple<DbLineVersion, DbRoute?, List<DbOperatingPeriod>>
    ): DbJourney {
        val optionalSaved = journeyJpaRepository.findByExternalIdAndLineIdAndValidRange(
            externalId = journey.journeyId.value,
            lineExternalId = journey.lineVersion.lineId.value,
            validFrom = journey.lineVersion.validIn.from,
            validTo = journey.lineVersion.validIn.to,
            timezone = journey.lineVersion.validIn.timezone,
            isDetour = journey.lineVersion.isDetour,
        )
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            if (journey.journeyPatternId.value != saved.journeyPatternId) {
                logDifference("journey pattern id", saved.journeyPatternId, journey.journeyPatternId.value, saved)
            }
            compareScheduledStops(journey, saved)
            compareOperatingPeriods(journey, saved)
            if (journey.nextDayFirstStopIndex != saved.nextDayFirstStopIndex) {
                logDifference("next day first stop index", saved.nextDayFirstStopIndex, journey.nextDayFirstStopIndex, saved)
            }
            return saved
        }
        val (lineVersion, route, operatingPeriods) = dependenciesSupplier()
        return toDb(journey, null, lineVersion, route, operatingPeriods)
    }
    
    private fun compareScheduledStops(journey: Journey, savedJourney: DbJourney) {
        val sortedSavedStops = savedJourney.schedule.sortedBy { it.stopId.stopOrder }
        if (journey.schedule.size != sortedSavedStops.size) logDifference("number of scheduled stops", sortedSavedStops.size, journey.schedule.size, savedJourney)
        for ((new, old) in journey.schedule.zip(sortedSavedStops)) {
            if (new.name != old.name) {
                logDifference("name", old.name, new.name, savedJourney, old)
            }
            if (new.arrival?.toNanoOfDay() != old.arrival?.toNanoOfDay()) {
                logDifference("arrival time", old.arrival, new.arrival, savedJourney, old)
            }
            if (new.departure?.toNanoOfDay() != old.departure?.toNanoOfDay()) {
                logDifference("departure time", old.departure, new.departure, savedJourney, old)
            }
        }
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

    private fun compareOperatingPeriods(journey: Journey, savedJourney: DbJourney) {
        val sortedNew = journey.operatingPeriods.sortedWith(operatingPeriodComparator(OperatingPeriod::fromDate, OperatingPeriod::toDate, OperatingPeriod::timezone, OperatingPeriod::validDays))
        val sortedOld = savedJourney.operatingPeriods.sortedWith(operatingPeriodComparator(DbOperatingPeriod::fromDate, DbOperatingPeriod::toDate, DbOperatingPeriod::timezone, DbOperatingPeriod::validDays))
        if (sortedNew.size != sortedOld.size) {
            logDifference("number of scheduled stops", sortedOld.size, sortedNew.size, savedJourney)
            return
        }
        for ((new, old) in sortedNew.zip(sortedOld)) {
            if (!new.fromDate.equals(old.fromDate)) {
                logDifference("operating period: from date", old.fromDate, new.fromDate, savedJourney)
            }
            if (!new.toDate.equals(old.toDate)) {
                logDifference("operating period: to date", old.toDate, new.toDate, savedJourney)
            }
            if (new.timezone.id != old.timezone.id) {
                logDifference("operating period: timezone", old.timezone.id, new.timezone.id, savedJourney)
            }
            if (new.validDays != old.validDays) {
                logDifference("operating period: valid days", old.validDays, new.validDays, savedJourney)
            }
        }
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, contextJourney: DbJourney, contextStop: DbScheduledStop) {
        logDifference("stop [${contextStop.stopId.stopOrder}]: $fieldName", old, new, contextJourney)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbJourney) {
        log.warn(
            "Journey {} for line version {} {} - {}({}): {} changed from {} to {}",
            context.externalId,
            context.lineVersion.externalId,
            context.lineVersion.validFrom,
            context.lineVersion.validTo,
            context.lineVersion.timezone,
            fieldName,
            old,
            new
        )
    }
}
