package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.domain.port.RoutingServicePort
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase.Companion.distanceBetweenPoints
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase.Companion.externalIdFromWaypoints
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
import org.mockito.quality.Strictness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class CalculateRoutesFromWaypointsTest {
    private lateinit var mockRoutingServicePort: RoutingServicePort
    private lateinit var mockPhysicalStopJpaRepository: PhysicalStopJpaRepository
    private lateinit var mockRouteJpaRepository: RouteJpaRepository
    private lateinit var sut: CalculateRoutesFromWaypoints
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

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

    @BeforeEach
    fun setup() {
        mockRoutingServicePort = mock(strictness = Strictness.LENIENT) {
            on { getRouteForPoints(any()) } doAnswer { invocation ->
                val coordinates = invocation.getArgument<List<Coordinate>>(0)
                RoutingServicePort.RoutingResponse(
                    route = coordinates,
                    distance = coordinates.zipWithNext().sumOf { distanceBetweenPoints(it.first, it.second) },
                    waypoints = coordinates,
                )
            }
        }
        mockPhysicalStopJpaRepository = mock()
        mockRouteJpaRepository = mock()
        sut = CalculateRoutesFromWaypoints(mockRoutingServicePort, mockPhysicalStopJpaRepository, mockRouteJpaRepository)
    }

    @Test
    fun `should call routing service and calculate route details when route doesn't exist`() {
        val waypoints = listOf(
            physicalStopTemplate("A", Coordinate(1.0, 2.0)),
            physicalStopTemplate("B", Coordinate(2.0, 2.0)),
            physicalStopTemplate("C", Coordinate(3.0, 2.0)),
        )

        whenever { mockRouteJpaRepository.findAllByExternalId(any()) } doReturn emptyList()
        whenever { mockPhysicalStopJpaRepository.findAllByExternalIds(any()) } doReturn emptyList()

        val routeCalculationCache = RouteCalculationCache()
        val result = sut.calculateRouteFromWaypoints(waypoints, routeCalculationCache)

        assertNotNull(result)
        assertEquals(result, routeCalculationCache.routes.firstOrNull())
        assertNull(result.relationalId)
        assertEquals(3, result.routeStops.size)
        for ((idx, physicalStop) in waypoints.zip(listOf(0.0, 0.5, 1.0)).withIndex()) {
            assertEquals(physicalStop.second, result.routeStops[idx].routeFraction)
            assertEquals(physicalStop.first, result.routeStops[idx].physicalStop)
            assertEquals(physicalStop.first, routeCalculationCache.findPhysicalStop(physicalStop.first.externalId))
        }
    }

    @Test
    fun `should return existing route from DB`() {
        val waypoints = listOf(
            physicalStopTemplate("A", Coordinate(1.0, 2.0)),
            physicalStopTemplate("B", Coordinate(2.0, 2.0)),
            physicalStopTemplate("C", Coordinate(3.0, 2.0)),
        )
        val existingRoute = Route(
            relationalId = 1L,
            externalId = externalIdFromWaypoints(waypoints),
            routeStops = emptyList(),
            totalDistance = 2.0,
            pointSequence = geometryFactory.createLineString(waypoints.map { it.position.coordinate }.toTypedArray()),
        )
        existingRoute.routeStops = listOf(
            RouteStop(
                stopId = RouteStopId(existingRoute.relationalId, 0),
                route = existingRoute,
                physicalStop = waypoints[0],
                routeFraction = 0.0,
            ),
            RouteStop(
                stopId = RouteStopId(existingRoute.relationalId, 1),
                route = existingRoute,
                physicalStop = waypoints[1],
                routeFraction = 0.5,
            ),
            RouteStop(
                stopId = RouteStopId(existingRoute.relationalId, 2),
                route = existingRoute,
                physicalStop = waypoints[2],
                routeFraction = 1.0,
            ),
        )

        whenever { mockRouteJpaRepository.findAllByExternalId(any()) } doAnswer { invocation ->
            val externalId = invocation.getArgument<String>(0)
            if (externalId == existingRoute.externalId) listOf(existingRoute) else emptyList()
        }

        val routeCalculationCache = RouteCalculationCache()
        val result = sut.calculateRouteFromWaypoints(waypoints, routeCalculationCache)

        assertNotNull(result)
        assertEquals(existingRoute, result)
        assertEquals(existingRoute, routeCalculationCache.routes.firstOrNull())
        for ((idx, existingRouteStop) in existingRoute.routeStops.withIndex()) {
            assertEquals(existingRouteStop.routeFraction, result.routeStops[idx].routeFraction)
            assertEquals(existingRouteStop.physicalStop, result.routeStops[idx].physicalStop)
            assertEquals(existingRouteStop.physicalStop, routeCalculationCache.findPhysicalStop(existingRouteStop.physicalStop.externalId))
        }
    }

    @Test
    fun `should return existing physical stops from DB`() {
        val waypoints = listOf(
            physicalStopTemplate("A", Coordinate(2.0, 2.0), relationalId = null),
            physicalStopTemplate("B", Coordinate(3.0, 2.0), relationalId = null),
            physicalStopTemplate("C", Coordinate(4.0, 2.0), relationalId = null),
        )
        val savedStops = listOf(
            physicalStopTemplate("A", Coordinate(1.0, 2.0), relationalId = 1),
            physicalStopTemplate("B", Coordinate(2.0, 2.0), relationalId = 2),
            physicalStopTemplate("C", Coordinate(3.0, 2.0), relationalId = 3),
        )
        val expectedWaypointFractions = listOf(0.0, 0.5, 1.0)

        whenever { mockRouteJpaRepository.findAllByExternalId(any()) } doReturn emptyList()
        whenever { mockPhysicalStopJpaRepository.findAllByExternalIds(any()) } doAnswer { invocation ->
            val externalIds = invocation.getArgument<List<String>>(0)
            savedStops.filter { it.externalId in externalIds }
        }

        val routeCalculationCache = RouteCalculationCache()
        val result = sut.calculateRouteFromWaypoints(waypoints, routeCalculationCache)

        assertNotNull(result)
        assertEquals(result, routeCalculationCache.routes.firstOrNull())
        for ((idx, savedStop) in savedStops.zip(expectedWaypointFractions).withIndex()) {
            assertEquals(savedStop.second, result.routeStops[idx].routeFraction)
            assertEquals(savedStop.first, result.routeStops[idx].physicalStop)
            assertEquals(savedStop.first, routeCalculationCache.findPhysicalStop(savedStop.first.externalId))
        }
    }

    @Test
    fun `should handle waypoints with same coordinates`() {
        val waypoints = listOf(
            physicalStopTemplate("A", Coordinate(1.0, 2.0)),
            physicalStopTemplate("B", Coordinate(1.0, 2.0)),
            physicalStopTemplate("C", Coordinate(3.0, 2.0)),
            physicalStopTemplate("D", Coordinate(3.0, 2.0)),
        )

        whenever { mockRouteJpaRepository.findAllByExternalId(any()) } doReturn emptyList()
        whenever { mockPhysicalStopJpaRepository.findAllByExternalIds(any()) } doReturn emptyList()

        val routeCalculationCache = RouteCalculationCache()
        val result = sut.calculateRouteFromWaypoints(waypoints, routeCalculationCache)

        assertNotNull(result)
        assertEquals(result, routeCalculationCache.routes.firstOrNull())
        assertNull(result.relationalId)
        assertEquals(4, result.routeStops.size)
        for ((idx, physicalStop) in waypoints.zip(listOf(0.0, 0.0, 1.0, 1.0)).withIndex()) {
            assertEquals(physicalStop.second, result.routeStops[idx].routeFraction)
            assertEquals(physicalStop.first, result.routeStops[idx].physicalStop)
            assertEquals(physicalStop.first, routeCalculationCache.findPhysicalStop(physicalStop.first.externalId))
        }
    }
}
