package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.TariffStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.JourneyPatternRoutingDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.JourneyPatternStopRoutingDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.TariffStopRoutingDto
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsSourcePort
import cz.cvut.fit.gaierda1.domain.port.ServiceUnavailableException
import cz.cvut.fit.gaierda1.domain.usecase.load.AddJrUtilPositionToStopsByNameUseCase.AddPositionToStopsByNameResult
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class EnrichBySpacialData(
    private val journeyPatternJpaRepository: JourneyPatternJpaRepository,
    private val journeyPatternStopJpaRepository: JourneyPatternStopJpaRepository,
    private val tariffStopJpaRepository: TariffStopJpaRepository,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    @PersistenceContext
    private val entityManager: EntityManager,
): EnrichBySpacialDataUseCase {
    private val log = LoggerFactory.getLogger(EnrichBySpacialData::class.java)

    override fun enrichStopsWithPositions(
        jrUtilGtfsSourcePort: JrUtilGtfsSourcePort,
        jrUtilGtfsParserPort: JrUtilGtfsParserPort,
        normalizeStopNameUseCase: NormalizeStopNameUseCase,
        addJrUtilPositionToStopsByNameUseCase: AddJrUtilPositionToStopsByNameUseCase,
        calculateRoutesFromWaypointsUseCase: CalculateRoutesFromWaypointsUseCase,
    ) {
        val jrUtilGtfsStreamsIterator = jrUtilGtfsSourcePort.provideInput().iterator()
        val jrUtilGtfsParseResult = jrUtilGtfsParserPort.parseGtfs(jrUtilGtfsStreamsIterator)
        for (parsedStop in jrUtilGtfsParseResult.allStops) {
            parsedStop.externalId = "JRUTIL:${parsedStop.externalId}"
        }
        val positionAssignments = addJrUtilPositionToStopsByNameUseCase
            .addPositionToStopsByName(jrUtilGtfsParseResult)
            .associateBy(AddPositionToStopsByNameResult::linePublicCode)
        val patternsCount: Int
        val journeyPatternWithNullRoute = journeyPatternJpaRepository
            .findAllRoutingDtoWithNullRoute()
            .also { patternsCount = it.size }
            .groupBy(JourneyPatternRoutingDto::linePublicCode)
            .mapValues { (_, journeyPatterns) ->
                journeyPatterns
                    .groupBy(JourneyPatternRoutingDto::lineVersionId)
                    .mapValues { (_, journeyPatterns) -> journeyPatterns.map(JourneyPatternRoutingDto::patternNumber) }
            }

        var patternsWithoutSpacialData = 0
        var patternsExtendingOutOfSupportedArea = 0
        try {
            for ((publicCode, patternNumbers) in journeyPatternWithNullRoute) {
                val positionAssignmentsForPublicCode = positionAssignments[publicCode]?.assignmentsByStopId
                if (positionAssignmentsForPublicCode == null) {
                    patternsWithoutSpacialData += patternNumbers.size
                    continue
                }
                for ((lineVersionId, patternNumbers) in patternNumbers) {
                    transactionTemplate.executeWithoutResult {
                        val tariffStopsForLine = tariffStopJpaRepository
                            .findAllRoutingDtoByLineVersionId(lineVersionId)
                            .sortedBy(TariffStopRoutingDto::tariffOrder)
                        val stopAssignments = journeyPatternStopJpaRepository
                            .findAllRoutingDtoByLineVersionId(lineVersionId)
                            .filter { it.patternNumber in patternNumbers }
                            .groupBy(JourneyPatternStopRoutingDto::patternNumber)
                            .mapValues { (_, stops) ->
                                stops.sortedBy(JourneyPatternStopRoutingDto::stopOrder)
                                    .map { positionAssignmentsForPublicCode[tariffStopsForLine[it.tariffOrder].stopId] }
                            }
                        val routingCache = RouteCalculationCache()
                        val patternRoutePairs = stopAssignments.mapNotNull { (patternNumber, jrStops) ->
                            val filteredStops = jrStops.mapNotNull { stop ->
                                if (stop == null || stop.position.x == 0.0 || stop.position.y == 0.0) null
                                else stop
                            }
                            if (filteredStops.size != jrStops.size) {
                                patternsWithoutSpacialData++
                                return@mapNotNull null
                            }
                            val route = calculateRoutesFromWaypointsUseCase
                                .calculateRouteFromWaypoints(filteredStops, routingCache)
                            if (route == null) {
                                patternsExtendingOutOfSupportedArea++
                                return@mapNotNull null
                            }
                            patternNumber to route
                        }
                        physicalStopJpaRepository.saveAll(routingCache.physicalStops.filter { it.relationalId == null })
                        val newRoutes = routingCache.routes.filter { it.relationalId == null }
                        routeJpaRepository.saveAll(newRoutes)
                        routeStopJpaRepository.saveAll(newRoutes.flatMap { it.routeStops })
                        entityManager.flush()
                        for ((patternNumber, route) in patternRoutePairs) {
                            journeyPatternJpaRepository.setRouteForJourneyPatternById(
                                lineVersionId,
                                patternNumber,
                                route.relationalId!!
                            )
                        }
                    }
                }
            }
        } catch (e: ServiceUnavailableException) {
            log.error("Error while calculating route", e)
            return
        }
        log.info("Tried to enrich $patternsCount journey patterns with spacial data.\n" +
                "There were $patternsWithoutSpacialData journey patterns without spacial data.\n" +
                "There were $patternsExtendingOutOfSupportedArea journey patterns extending out of supported area.")
    }
}
