package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.OsmParserPort
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
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ImportPhysicalStopsFromOsmTest(
) {
    private lateinit var mockPhysicalStopJpaRepository: PhysicalStopJpaRepository
    private lateinit var mockTransactionTemplate: TransactionTemplate
    private lateinit var mockOsmParser: OsmParserPort
    private lateinit var mockNormalizeStopName: NormalizeStopNameUseCase
    private lateinit var sut: ImportPhysicalStopsFromOsm
    private val batchSize = 6
    private val physicalStopCount = batchSize * 2
    private val physicalStopSavedModulus = 9
    private val savedStopsExternalIdPrefix = "OSM:"
    private var savedPhysicalStopsInTransaction = mutableListOf<MutableList<PhysicalStop>>()
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun parsedStops() = (0 until physicalStopCount).map {
        PhysicalStop(
            relationalId = null,
            externalId = "ID$it",
            name = "Stop $it",
            position = geometryFactory.createPoint(Coordinate(it.toDouble(), it.toDouble())),
            tags = emptyMap(),
        )
    }
    private fun savedStops() = (0 until physicalStopCount step physicalStopSavedModulus).map {
        PhysicalStop(
            relationalId = it.toLong(),
            externalId = "${savedStopsExternalIdPrefix}ID$it",
            name = "Stop $it",
            position = geometryFactory.createPoint(Coordinate(it.toDouble(), it.toDouble())),
            tags = emptyMap(),
        )
    }

    @BeforeEach
    fun setup() {
        savedPhysicalStopsInTransaction = mutableListOf()
        mockPhysicalStopJpaRepository = mock {
            on { findAllByExternalIds(any()) } doAnswer { invocation ->
                val externalIds = invocation.getArgument<List<String>>(0)
                savedStops().filter { it.externalId in externalIds }
            }
            on { saveAll<PhysicalStop>(any()) } doAnswer { invocation ->
                val savedBatch = invocation.getArgument<List<PhysicalStop>>(0)
                savedPhysicalStopsInTransaction.last().addAll(savedBatch)
                savedBatch
            }
        }
        mockTransactionTemplate = mock {
            on { executeWithoutResult(any()) }.then { invocation ->
                savedPhysicalStopsInTransaction.add(mutableListOf())
                val consumer = invocation.getArgument<Consumer<TransactionStatus>>(0)
                consumer.accept(mock())
            }
        }
        mockOsmParser = mock {
            on { parseOsmFile(any()) } doReturn parsedStops()
        }
        mockNormalizeStopName = mock {
            on { normalize(any()) } doAnswer { invocation -> invocation.getArgument(0) }
        }
        sut = ImportPhysicalStopsFromOsm(
            mockPhysicalStopJpaRepository,
            mockTransactionTemplate,
            batchSize,
        )
    }

    @Test
    fun `should not save more physical stops than the batch size in one transaction`() {
        sut.importPhysicalStopsFromOsm(mock(), mockOsmParser, mockNormalizeStopName)

        for (batch in savedPhysicalStopsInTransaction) {
            assertTrue(batch.size <= batchSize)
        }
    }

    @Test
    fun `should save all parsed physical stops and add prefix to external ids`() {
        sut.importPhysicalStopsFromOsm(mock(), mockOsmParser, mockNormalizeStopName)

        val originalParsedStops = parsedStops()
        val savedStops = savedPhysicalStopsInTransaction
            .flatMap { it }
            .associateBy { it.externalId }
        for (originalParsedStop in originalParsedStops) {
            assertContains(savedStops, "${savedStopsExternalIdPrefix}${originalParsedStop.externalId}")
        }
    }

    @Test
    fun `should save existing physical stops with set relational id`() {
        sut.importPhysicalStopsFromOsm(mock(), mockOsmParser, mockNormalizeStopName)

        val existingStops = savedStops()
        val savedStops = savedPhysicalStopsInTransaction
            .flatMap { it }
            .associateBy { it.externalId }
        for (existingStop in existingStops) {
            assertContains(savedStops, existingStop.externalId)
            val savedStop = savedStops[existingStop.externalId]!!
            assertEquals(existingStop.relationalId, savedStop.relationalId)
        }
    }

    @Test
    fun `should save new physical stops with null relational id`() {
        sut.importPhysicalStopsFromOsm(mock(), mockOsmParser, mockNormalizeStopName)

        val existingStops = savedStops()
        val newStops = parsedStops()
            .apply { forEach { newStop -> newStop.externalId = "${savedStopsExternalIdPrefix}${newStop.externalId}" } }
            .filter { newStop -> existingStops.none { existingStop -> newStop.externalId == existingStop.externalId } }
        val savedStops = savedPhysicalStopsInTransaction
            .flatMap { it }
            .associateBy { it.externalId }
        for (newStop in newStops) {
            assertContains(savedStops, newStop.externalId)
            val savedStop = savedStops[newStop.externalId]!!
            assertNull(savedStop.relationalId)
        }
    }
}
