package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateTimeRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
open class JourneyRepositoryAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val operatingPeriodRepositoryAdapter: OperatingPeriodRepositoryAdapter,
    private val lineVersionRepositoryAdapter: LineVersionRepositoryAdapter,
    private val routeRepositoryAdapter: RouteRepositoryAdapter,
): JourneyRepository {
    fun toDomain(journey: DbJourney): Journey = Journey(
        journeyId = JourneyId(journey.externalId),
        lineVersion = lineVersionRepositoryAdapter.toDomain(journey.lineVersion),
        journeyPatternId = JourneyPatternId(journey.journeyPatternId),
        schedule = journey.schedule.sortedBy { it.stopId.stopOrder }.map(::toDomain),
        operatingPeriods = journey.operatingPeriods.map(operatingPeriodRepositoryAdapter::toDomain),
        route = journey.route?.let(routeRepositoryAdapter::toDomain)
    )

    fun toDomain(scheduledStop: DbScheduledStop): ScheduledStop = ScheduledStop(
        name = scheduledStop.name,
        stopOnRequest = scheduledStop.stopOnRequest,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    fun toDb(
        journey: Journey,
        relationalId: Long?,
        lineVersion: DbLineVersion,
        route: DbRoute?,
        operatingPeriods: List<DbOperatingPeriod>,
    ): DbJourney {
        val schedule = mutableListOf<DbScheduledStop>()
        val dbJourney = DbJourney(
            relationalId = relationalId,
            externalId = journey.journeyId.value,
            journeyPatternId = journey.journeyPatternId.value,
            lineVersion = lineVersion,
            route = route,
            schedule = schedule,
            operatingPeriods = operatingPeriods,
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

    fun findOrMap(
        journey: Journey,
        dependenciesSupplier: () -> Triple<DbLineVersion, DbRoute?, List<DbOperatingPeriod>>,
    ): DbJourney {
        val optionalSaved = journeyJpaRepository.findByExternalIdAndLineIdAndValidRange(
            externalId = journey.journeyId.value,
            lineExternalId = journey.lineVersion.lineId.value,
            validFrom = journey.lineVersion.validIn.from,
            validTo = journey.lineVersion.validIn.to,
            timezone = journey.lineVersion.validIn.timezone,
            isDetour = journey.lineVersion.isDetour,
        )
        return optionalSaved.orElseGet {
            val (lineVersion, route, operatingPeriods) = dependenciesSupplier()
            toDb(journey, null, lineVersion, route, operatingPeriods)
        }
    }

    fun saveDb(journey: DbJourney) {
        journeyJpaRepository.save(journey)
        scheduledStopJpaRepository.saveAll(journey.schedule)
    }

    fun saveAllDb(journeys: Iterable<DbJourney>) {
        journeyJpaRepository.saveAll(journeys)
        scheduledStopJpaRepository.saveAll(journeys.flatMap { it.schedule })
    }

    private fun dependenciesSupplierFor(journey: Journey) = {
        Triple(
            lineVersionRepositoryAdapter.findSaveMapping(journey.lineVersion),
            journey.route?.let(routeRepositoryAdapter::findSaveMapping),
            operatingPeriodRepositoryAdapter.findSaveMappings(journey.operatingPeriods),
        )
    }

    fun findSaveMapping(journey: Journey): DbJourney {
        val mapped = findOrMap(journey, dependenciesSupplierFor(journey))
        if (mapped.relationalId == null) saveDb(mapped)
        return mapped
    }

    private val journeyComparator = compareBy<Journey> { it.journeyId.value }
        .thenBy { it.lineVersion.lineId.value }
        .thenBy { it.lineVersion.validIn.from }
        .thenBy { it.lineVersion.validIn.to }
        .thenBy { it.lineVersion.isDetour }
        .thenBy { it.lineVersion.validIn.timezone.id }

    private fun findSaveMappingsImpl(journeys: Iterable<Journey>, result: Boolean): List<DbJourney>? {
        val uniqueJourneys = sortedSetOf(comparator = journeyComparator)
        uniqueJourneys.addAll(journeys)
        lineVersionRepositoryAdapter.saveAllIfAbsent(uniqueJourneys.map { it.lineVersion })
        operatingPeriodRepositoryAdapter.saveAllIfAbsent(uniqueJourneys.flatMap { it.operatingPeriods })
        routeRepositoryAdapter.saveAllIfAbsent(uniqueJourneys.mapNotNull { it.route })

        val mappedUniqueJourneys = uniqueJourneys.map { journey ->
            findOrMap(journey, dependenciesSupplierFor(journey))
        }
        saveAllDb(mappedUniqueJourneys.filter { it.relationalId == null })
        return if (result) journeys.map { domainJourney -> mappedUniqueJourneys.find { dbJourney ->
                    domainJourney.journeyId.value == dbJourney.externalId
                        && domainJourney.lineVersion.lineId.value == dbJourney.lineVersion.externalId
                        && domainJourney.lineVersion.validIn.from.equals(dbJourney.lineVersion.validFrom)
                        && domainJourney.lineVersion.validIn.to.equals(dbJourney.lineVersion.validTo)
                        && domainJourney.lineVersion.isDetour == dbJourney.lineVersion.isDetour
                        && domainJourney.lineVersion.validIn.timezone.id == dbJourney.lineVersion.timezone.id
                }!! }
            else null
    }

    fun findSaveMappings(journeys: Iterable<Journey>): List<DbJourney> {
        return findSaveMappingsImpl(journeys, true)!!
    }

    override fun saveIfAbsent(journey: Journey) {
        findSaveMapping(journey)
    }

    override fun saveAllIfAbsent(journeys: Iterable<Journey>) {
        findSaveMappingsImpl(journeys, false)
    }

    override fun findById(lineId: LineId, validRange: DateTimeRange, isDetour: Boolean, journeyId: JourneyId): Journey? {
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

    override fun getPage(pageable: Pageable): Page<Journey> {
        return journeyJpaRepository.findAll(pageable).map(::toDomain)
    }
}
