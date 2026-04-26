package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
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
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class EnrichBySpacialDataTest {
    private lateinit var mockJourneyPatternJpaRepository: JourneyPatternJpaRepository
    private lateinit var mockJourneyPatternStopJpaRepository: JourneyPatternStopJpaRepository
    private lateinit var mockTariffStopJpaRepository: TariffStopJpaRepository
    private lateinit var mockPhysicalStopJpaRepository: PhysicalStopJpaRepository
    private lateinit var mockRouteJpaRepository: RouteJpaRepository
    private lateinit var mockRouteStopJpaRepository: RouteStopJpaRepository
    private lateinit var mockTransactionTemplate: TransactionTemplate
    private lateinit var mockEntityManager: EntityManager

    private lateinit var mockJrUtilGtfsSourcePort: JrUtilGtfsSourcePort
    private lateinit var mockJrUtilGtfsParserPort: JrUtilGtfsParserPort
    private lateinit var mockNormalizeStopNameUseCase: NormalizeStopNameUseCase
    private lateinit var mockRoughlyPairPhysicalStopsWithStopsUseCase: RoughlyPairPhysicalStopsWithStopsUseCase
    private lateinit var mockPairJrUtilStopsWithStopsUseCase: PairJrUtilStopsWithStopsUseCase
    private lateinit var mockCalculateRoutesFromWaypointsUseCase: CalculateRoutesFromWaypointsUseCase

    private lateinit var sut: EnrichBySpacialData
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    private val jrUtilStopExternalIdPrefix = "JRUTIL:"

    private data class InTransactionSavedEntities(
        val physicalStops: MutableList<PhysicalStop>,
        val routes: MutableList<Route>,
        val routeStops: MutableList<RouteStop>,
    )
    private val savedEntitiesInTransaction = mutableListOf<InTransactionSavedEntities>()

    private fun physicalStopTemplate(
        externalId: String,
        coordinate: Coordinate,
        relationalId: Long? = null,
    ) = PhysicalStop(
        relationalId = relationalId,
        externalId = externalId,
        name = "",
        position = geometryFactory.createPoint(coordinate),
        tags = emptyMap(),
    )

    private fun routeTemplate(relationalId: Long?, stopCount: Int): Route {
        val res = Route(
            relationalId = relationalId,
            externalId = "",
            pointSequence = geometryFactory.createLineString(),
            routeStops = emptyList(),
            totalDistance = 1.0,
        )
        res.routeStops = (0 until stopCount).map { RouteStop(
            stopId = RouteStopId(relationalId, it),
            route = res,
            physicalStop = mock(),
            routeFraction = 1.0,
        ) }
        return res
    }

    @BeforeEach
    fun setup() {
        mockJourneyPatternStopJpaRepository = mock()
        mockTariffStopJpaRepository = mock()
        mockTransactionTemplate = mock {
            on { executeWithoutResult(any()) }.then { invocation ->
                savedEntitiesInTransaction.add(InTransactionSavedEntities(mutableListOf(), mutableListOf(), mutableListOf()))
                val consumer = invocation.getArgument<Consumer<TransactionStatus>>(0)
                consumer.accept(mock())
            }
        }
        mockPhysicalStopJpaRepository = mock {
            on { saveAll<PhysicalStop>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<PhysicalStop>>(0).toList()
                savedEntitiesInTransaction.last().physicalStops.addAll(elements)
                elements
            }
        }
        mockRouteJpaRepository = mock {
            on { saveAll<Route>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<Route>>(0).toList()
                savedEntitiesInTransaction.last().routes.addAll(elements.map {
                    Route(it.relationalId, it.externalId, it.pointSequence, it.routeStops, it.totalDistance)
                })
                elements.forEach { route -> route.relationalId = 1L }
                elements
            }
        }
        mockRouteStopJpaRepository = mock {
            on { saveAll<RouteStop>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<RouteStop>>(0).toList()
                savedEntitiesInTransaction.last().routeStops.addAll(elements)
                elements
            }
        }
        mockJourneyPatternJpaRepository = mock {
            on { setRouteForJourneyPatternById(any(), any(), any()) } doAnswer { }
        }
        mockEntityManager = mock {
            on { flush() } doAnswer { }
        }
        sut = EnrichBySpacialData(
            mockJourneyPatternJpaRepository,
            mockJourneyPatternStopJpaRepository,
            mockTariffStopJpaRepository,
            mockPhysicalStopJpaRepository,
            mockRouteJpaRepository,
            mockRouteStopJpaRepository,
            mockTransactionTemplate,
            mockEntityManager,
        )
        mockJrUtilGtfsSourcePort = mock {
            on { provideInput() } doReturn emptySequence()
        }
        mockNormalizeStopNameUseCase = mock()
        mockRoughlyPairPhysicalStopsWithStopsUseCase = mock()
        mockJrUtilGtfsParserPort = mock()
        mockPairJrUtilStopsWithStopsUseCase = mock()
        mockCalculateRoutesFromWaypointsUseCase = mock()
    }

    @Test
    fun `should save only new entities`() {
        val linePublicCode = "ABC"
        val lineVersionId = 1L
        val newPhysicalStopExternalId = "B"
        val physicalStops = listOf(
            physicalStopTemplate("A", Coordinate(1.0, 2.0), 1L),
            physicalStopTemplate(newPhysicalStopExternalId, Coordinate(2.0, 2.0), null),
            physicalStopTemplate("C", Coordinate(3.0, 2.0), 2L),
        )
        val journeyPatterns = listOf(
            JourneyPatternRoutingDto(lineVersionId, 0, linePublicCode),
            JourneyPatternRoutingDto(lineVersionId, 1, linePublicCode),
        )
        val tariffStops = listOf(
            TariffStopRoutingDto(0, 1L),
            TariffStopRoutingDto(1, 2L),
            TariffStopRoutingDto(2, 3L),
        )
        val journeyPatternStops = listOf(
            JourneyPatternStopRoutingDto(0, 0, 0),
            JourneyPatternStopRoutingDto(0, 1, 2),
            JourneyPatternStopRoutingDto(1, 0, 0),
            JourneyPatternStopRoutingDto(1, 1, 1),
            JourneyPatternStopRoutingDto(1, 2, 2),
        )

        whenever { mockJrUtilGtfsParserPort.parseGtfs(any()) } doReturn JrUtilGtfsParserPort.JrUtilGtfsParseResult(
            listOf(JrUtilGtfsParserPort.JrUtilGtfsLineParseResult(linePublicCode, physicalStops)),
            physicalStops
        )
        whenever { mockJourneyPatternJpaRepository.findAllRoutingDtoWithNullRoute() } doReturn journeyPatterns
        whenever { mockTariffStopJpaRepository.findAllRoutingDtoByLineVersionId(any()) } doReturn tariffStops
        whenever { mockJourneyPatternStopJpaRepository.findAllRoutingDtoByLineVersionId(any()) } doReturn journeyPatternStops
        whenever { mockPairJrUtilStopsWithStopsUseCase.pairJrUtilStopsWithStops(any(), any()) } doReturn listOf(
            PairJrUtilStopsWithStopsUseCase.PairJrUtilStopsWithStopsResult(
                linePublicCode,
                mapOf(
                    1L to physicalStops[0],
                    2L to physicalStops[1],
                    3L to physicalStops[2],
                ),
            )
        )
        whenever { mockCalculateRoutesFromWaypointsUseCase.calculateRouteFromWaypoints(any(), any()) } doAnswer { invocation ->
            val waypoints = invocation.getArgument<List<PhysicalStop>>(0)
            val cache = invocation.getArgument<RouteCalculationCache>(1)
            val route: Route
            if (waypoints.size == 2) {
                cache.addPhysicalStop(physicalStops[0])
                cache.addPhysicalStop(physicalStops[2])
                route = routeTemplate(1L, 2)
                cache.addRoute(route)
            } else {
                route = routeTemplate(null, 3)
                cache.addPhysicalStop(physicalStops[1])
                cache.addRoute(route)
            }
            route
        }

        sut.enrichStopsWithPositions(
            mockJrUtilGtfsSourcePort,
            mockJrUtilGtfsParserPort,
            mockNormalizeStopNameUseCase,
            mockRoughlyPairPhysicalStopsWithStopsUseCase,
            mockPairJrUtilStopsWithStopsUseCase,
            mockCalculateRoutesFromWaypointsUseCase,
        )

        val savedEntities = savedEntitiesInTransaction.reduce { acc, cur ->
            acc.physicalStops.addAll(cur.physicalStops)
            acc.routes.addAll(cur.routes)
            acc.routeStops.addAll(cur.routeStops)
            acc
        }
        assertEquals(1, savedEntities.physicalStops.size)
        assertEquals("$jrUtilStopExternalIdPrefix$newPhysicalStopExternalId", savedEntities.physicalStops[0].externalId)
        assertNull(savedEntities.physicalStops[0].relationalId)
        assertEquals(1, savedEntities.routes.size)
        assertNull(savedEntities.routes[0].relationalId)
        assertEquals(3, savedEntities.routeStops.size)
    }
}
