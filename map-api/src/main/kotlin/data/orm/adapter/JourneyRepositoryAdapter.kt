package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.util.PageAdapter
import cz.cvut.fit.gaierda1.domain.model.DateTimeRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.Page
import cz.cvut.fit.gaierda1.domain.model.PageRequest
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.measuring.Measurer
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class JourneyRepositoryAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val operatingPeriodRepositoryAdapter: OperatingPeriodRepositoryAdapter,
    private val lineVersionRepositoryAdapter: LineVersionRepositoryAdapter,
    private val routeRepositoryAdapter: RouteRepositoryAdapter,
    private val physicalStopRepositoryAdapter: PhysicalStopRepositoryAdapter,
    private val pageAdapter: PageAdapter,
    private val entityManager: EntityManager,
): JourneyRepository {
    fun toDomain(journey: DbJourney): Journey = Journey(
        journeyId = JourneyId(journey.externalId),
        lineVersion = lineVersionRepositoryAdapter.toDomain(journey.lineVersion),
        journeyPatternId = JourneyPatternId(journey.journeyPatternId),
        schedule = journey.schedule.sortedBy { it.stopId.stopOrder }.map(::toDomain),
        operatingPeriods = journey.operatingPeriods.map(operatingPeriodRepositoryAdapter::toDomain),
        route = journey.route?.let(routeRepositoryAdapter::toDomain),
        nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
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
            nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
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

    fun findByDomainId(journey: Journey): Optional<DbJourney> = Measurer.addToDbFind {
        ++Measurer.searchedJourneys
        journeyJpaRepository
            .findByExternalIdAndLineIdAndValidRange(
                externalId = journey.journeyId.value,
                lineExternalId = journey.lineVersion.lineId.value,
                validFrom = journey.lineVersion.validIn.from,
                validTo = journey.lineVersion.validIn.to,
                timezone = journey.lineVersion.validIn.timezone,
                isDetour = journey.lineVersion.isDetour,
            )
    }

    fun findOrMap(
        journey: Journey,
        dependenciesSupplier: () -> Triple<DbLineVersion, DbRoute?, List<DbOperatingPeriod>>,
    ): DbJourney {
        val optionalSaved = findByDomainId(journey)
        return optionalSaved.orElseGet {
            val (lineVersion, route, operatingPeriods) = dependenciesSupplier()
            toDb(journey, null, lineVersion, route, operatingPeriods)
        }
    }

    fun saveDb(journey: DbJourney) {
        ++Measurer.savedJourneys
        Measurer.savedScheduledStops += journey.schedule.size
        Measurer.addToDbSave {
            journeyJpaRepository.save(journey)
            scheduledStopJpaRepository.saveAll(journey.schedule)
        }
    }

    fun saveAllDb(journeys: Iterable<DbJourney>) {
        Measurer.savedJourneys += journeys.count()
        Measurer.savedScheduledStops += journeys.sumOf { it.schedule.size }
        Measurer.addToDbSave {
            journeyJpaRepository.saveAll(journeys)
            scheduledStopJpaRepository.saveAll(journeys.flatMap { it.schedule })
        }
    }

    fun findSaveMapping(journey: Journey): FindSaveSingleMapping {
        var toSaveLineVersion: DbLineVersion? = null
        var toSaveRoute: DbRoute? = null
        var toSavePhysicalStopMappings = emptyList<DbPhysicalStop>()
        var toSaveOperatingPeriods = emptyList<DbOperatingPeriod>()
        val mapped = findOrMap(journey, {
            val lineVersionMapping = lineVersionRepositoryAdapter.findSaveMapping(journey.lineVersion)
            val routeMapping = journey.route?.let(routeRepositoryAdapter::findSaveMapping)
            val operatingPeriodsMapping = operatingPeriodRepositoryAdapter.findSaveMappings(journey.operatingPeriods)
            if (lineVersionMapping.save) {
                toSaveLineVersion = lineVersionMapping.lineVersion
            }
            if (routeMapping?.save == true) {
                toSaveRoute = routeMapping.route
                toSavePhysicalStopMappings = routeMapping.toSavePhysicalStops
            }
            toSaveOperatingPeriods = operatingPeriodsMapping.toSaveOperatingPeriods
            Triple(lineVersionMapping.lineVersion, routeMapping?.route, operatingPeriodsMapping.operatingPeriods)
        })
        return FindSaveSingleMapping(mapped, mapped.relationalId == null, toSaveLineVersion, toSaveRoute, toSavePhysicalStopMappings, toSaveOperatingPeriods)
    }

    private val journeyComparator = compareBy<Journey> { it.journeyId.value }
        .thenBy { it.lineVersion.lineId.value }
        .thenBy { it.lineVersion.validIn.from }
        .thenBy { it.lineVersion.validIn.to }
        .thenBy { it.lineVersion.isDetour }
        .thenBy { it.lineVersion.validIn.timezone.id }

    private data class FindSaveMultipleMappingImpl(
        val journeys: List<DbJourney>?,
        val toSaveJourneys: List<DbJourney>,
        val toSaveLineVersions: List<DbLineVersion>,
        val toSaveRoutes: List<DbRoute>,
        val toSavePhysicalStops: List<DbPhysicalStop>,
        val toSaveOperatingPeriods: List<DbOperatingPeriod>,
    )

    private fun findSaveMappingsImpl(journeys: Iterable<Journey>, result: Boolean): FindSaveMultipleMappingImpl {
        val uniqueJourneys = sortedSetOf(comparator = journeyComparator)
        uniqueJourneys.addAll(journeys)
        val mappedLineVersions = lineVersionRepositoryAdapter.findSaveMappings(uniqueJourneys.map { it.lineVersion })
        val operatingPeriodsCountPrefixSum = uniqueJourneys.map { it.operatingPeriods.size }.runningReduce(Int::plus)
        val mappedOperatingPeriods = operatingPeriodRepositoryAdapter.findSaveMappings(uniqueJourneys.flatMap { it.operatingPeriods })
        val routeIndexPrefixSum = uniqueJourneys.map { if (it.route != null) 1 else 0 }.runningReduce(Int::plus)
        val mappedRoutes = routeRepositoryAdapter.findSaveMappings(uniqueJourneys.mapNotNull { it.route })

        val mappedUniqueJourneys = uniqueJourneys.mapIndexed { idx, journey ->
            findOrMap(journey, {
                val prefixSum = operatingPeriodsCountPrefixSum[idx]
                Triple(
                    mappedLineVersions.lineVersions[idx],
                    journey.route?.let { mappedRoutes.routes[routeIndexPrefixSum[idx] - 1] },
                    mappedOperatingPeriods.operatingPeriods.subList(prefixSum - journey.operatingPeriods.size, prefixSum)
                )
            })
        }
        return FindSaveMultipleMappingImpl(
            (if (result) {
                journeys.map { domainJourney ->
                    mappedUniqueJourneys.find { dbJourney ->
                        domainJourney.journeyId.value == dbJourney.externalId
                                && domainJourney.lineVersion.lineId.value == dbJourney.lineVersion.externalId
                                && domainJourney.lineVersion.validIn.from.equals(dbJourney.lineVersion.validFrom)
                                && domainJourney.lineVersion.validIn.to.equals(dbJourney.lineVersion.validTo)
                                && domainJourney.lineVersion.isDetour == dbJourney.lineVersion.isDetour
                                && domainJourney.lineVersion.validIn.timezone.id == dbJourney.lineVersion.timezone.id
                    }!!
                }
            } else null),
            mappedUniqueJourneys.filter { it.relationalId == null },
            mappedLineVersions.toSaveLineVersions,
            mappedRoutes.toSaveRoutes,
            mappedRoutes.toSavePhysicalStops,
            mappedOperatingPeriods.toSaveOperatingPeriods,
        )
    }

    fun findSaveMappings(journeys: Iterable<Journey>): FindSaveMultipleMapping {
        val res = findSaveMappingsImpl(journeys, true)
        return FindSaveMultipleMapping(
            res.journeys!!,
            res.toSaveJourneys,
            res.toSaveLineVersions,
            res.toSaveRoutes,
            res.toSavePhysicalStops,
            res.toSaveOperatingPeriods,
        )
    }

    override fun saveIfAbsent(journey: Journey) {
        val mapping = findSaveMapping(journey)
        if (mapping.save) {
            if (mapping.toSavePhysicalStops.isNotEmpty()) {
                physicalStopRepositoryAdapter.saveAllDb(mapping.toSavePhysicalStops)
            }
            if (mapping.toSaveRoute != null) {
                routeRepositoryAdapter.saveDb(mapping.toSaveRoute)
            }
            if (mapping.toSaveOperatingPeriods.isNotEmpty()) {
                operatingPeriodRepositoryAdapter.saveAllDb(mapping.toSaveOperatingPeriods)
            }
            if (mapping.toSaveLineVersion != null) {
                lineVersionRepositoryAdapter.saveDb(mapping.toSaveLineVersion)
            }
            saveDb(mapping.journey)
        }
    }

    override fun saveAllIfAbsent(journeys: Iterable<Journey>) {
        val mapping = findSaveMappingsImpl(journeys, false)
        if (mapping.toSavePhysicalStops.isNotEmpty()) {
            physicalStopRepositoryAdapter.saveAllDb(mapping.toSavePhysicalStops)
        }
        if (mapping.toSaveRoutes.isNotEmpty()) {
            routeRepositoryAdapter.saveAllDb(mapping.toSaveRoutes)
        }
        if (mapping.toSaveOperatingPeriods.isNotEmpty()) {
            operatingPeriodRepositoryAdapter.saveAllDb(mapping.toSaveOperatingPeriods)
        }
        if (mapping.toSaveLineVersions.isNotEmpty()) {
            lineVersionRepositoryAdapter.saveAllDb(mapping.toSaveLineVersions)
        }
        if (mapping.toSaveJourneys.isNotEmpty()) {
            saveAllDb(mapping.toSaveJourneys)
        }
        Measurer.addToDbSave {
            entityManager.flush()
            entityManager.clear()
        }
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

    override fun save(journey: Journey) {
        val lineVersionMapping = lineVersionRepositoryAdapter.findSaveMapping(journey.lineVersion)
        val routeMapping = journey.route?.let(routeRepositoryAdapter::findSaveMapping)
        val operatingPeriodsMapping = operatingPeriodRepositoryAdapter.findSaveMappings(journey.operatingPeriods)

        val optionalSaved = findByDomainId(journey)
        val savedId = optionalSaved.map { it.relationalId }.orElse(null)
        val mapped = toDb(
            journey,
            savedId,
            lineVersionMapping.lineVersion,
            routeMapping?.route,
            operatingPeriodsMapping.operatingPeriods,
        )
        if (lineVersionMapping.save) {
            lineVersionRepositoryAdapter.saveDb(lineVersionMapping.lineVersion)
        }
        if (routeMapping?.save == true) {
            if (routeMapping.toSavePhysicalStops.isNotEmpty()) {
                physicalStopRepositoryAdapter.saveAllDb(routeMapping.toSavePhysicalStops)
            }
            routeRepositoryAdapter.saveDb(routeMapping.route)
        }
        if (operatingPeriodsMapping.toSaveOperatingPeriods.isNotEmpty()) {
            operatingPeriodRepositoryAdapter.saveAllDb(operatingPeriodsMapping.toSaveOperatingPeriods)
        }
        saveDb(mapped)
        optionalSaved.ifPresent { saved ->
            val scheduleLengthDiff = saved.schedule.size - journey.schedule.size
            if (scheduleLengthDiff > 0) {
                scheduledStopJpaRepository.deleteAll(saved.schedule.takeLast(scheduleLengthDiff))
            }
        }
    }

    override fun saveAll(journeys: Iterable<Journey>) {
        val mappedLineVersions = lineVersionRepositoryAdapter.findSaveMappings(journeys.map { it.lineVersion })
        val operatingPeriodsCountPrefixSum = journeys.map { it.operatingPeriods.size }.runningReduce(Int::plus)
        val mappedOperatingPeriods = operatingPeriodRepositoryAdapter.findSaveMappings(journeys.flatMap { it.operatingPeriods })
        val routeIndexPrefixSum = journeys.map { if (it.route != null) 1 else 0 }.runningReduce(Int::plus)
        val mappedRoutes = routeRepositoryAdapter.findSaveMappings(journeys.mapNotNull { it.route })

        val toDeleteScheduledStops = mutableListOf<DbScheduledStop>()
        val mappedJourneys = journeys.mapIndexed { idx, journey ->
            val optionalSaved = findByDomainId(journey)
            val savedId = optionalSaved.map { it.relationalId }.orElse(null)
            val lineVersion = mappedLineVersions.lineVersions[idx]
            val route = journey.route?.let { mappedRoutes.routes[routeIndexPrefixSum[idx] - 1] }
            val prefixSum = operatingPeriodsCountPrefixSum[idx]
            val operatingPeriods = mappedOperatingPeriods.operatingPeriods.subList(prefixSum - journey.operatingPeriods.size, prefixSum)
            val mapped = toDb(journey, savedId, lineVersion, route, operatingPeriods)
            optionalSaved.ifPresent { saved ->
                val scheduleLengthDiff = saved.schedule.size - journey.schedule.size
                if (scheduleLengthDiff > 0) {
                    toDeleteScheduledStops.addAll(saved.schedule.takeLast(scheduleLengthDiff))
                }
            }
            return@mapIndexed mapped
        }
        if (mappedRoutes.toSavePhysicalStops.isNotEmpty()) {
            physicalStopRepositoryAdapter.saveAllDb(mappedRoutes.toSavePhysicalStops)
        }
        if (mappedRoutes.toSaveRoutes.isNotEmpty()) {
            routeRepositoryAdapter.saveAllDb(mappedRoutes.toSaveRoutes)
        }
        if (mappedOperatingPeriods.toSaveOperatingPeriods.isNotEmpty()) {
            operatingPeriodRepositoryAdapter.saveAllDb(mappedOperatingPeriods.toSaveOperatingPeriods)
        }
        if (mappedLineVersions.toSaveLineVersions.isNotEmpty()) {
            lineVersionRepositoryAdapter.saveAllDb(mappedLineVersions.toSaveLineVersions)
        }
        saveAllDb(mappedJourneys)
        Measurer.addToDbSave {
            if (toDeleteScheduledStops.isNotEmpty()) {
                scheduledStopJpaRepository.deleteAll(toDeleteScheduledStops)
            }
            entityManager.flush()
            entityManager.clear()
        }
    }

    override fun findAll(pageRequest: PageRequest): Page<Journey> {
        return journeyJpaRepository
            .findAll(pageAdapter.toData(pageRequest))
            .map(::toDomain)
            .let(pageAdapter::toDomain)
    }

    override fun findAllWithNullRoute(pageRequest: PageRequest): Page<Journey> {
        return Measurer.addToDbFind {
            journeyJpaRepository
                .findByNullRoute(pageAdapter.toData(pageRequest))
        }
            .map(::toDomain)
            .let(pageAdapter::toDomain)
    }

    data class FindSaveSingleMapping(
        val journey: DbJourney,
        val save: Boolean,
        val toSaveLineVersion: DbLineVersion?,
        val toSaveRoute: DbRoute?,
        val toSavePhysicalStops: List<DbPhysicalStop>,
        val toSaveOperatingPeriods: List<DbOperatingPeriod>,
    )

    data class FindSaveMultipleMapping(
        val journeys: List<DbJourney>,
        val toSaveJourneys: List<DbJourney>,
        val toSaveLineVersions: List<DbLineVersion>,
        val toSaveRoutes: List<DbRoute>,
        val toSavePhysicalStops: List<DbPhysicalStop>,
        val toSaveOperatingPeriods: List<DbOperatingPeriod>,
    )
}
