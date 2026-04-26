package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.StopPositionEnrichmentDto
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsLineParseResult
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsParseResult
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
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class PairJrUtilStopsWithStopsTest {
    private lateinit var mockLineVersionJpaRepository: LineVersionJpaRepository
    private lateinit var mockStopJpaRepository: StopJpaRepository
    private lateinit var mockNormalizeStopName: NormalizeStopNameUseCase
    private lateinit var sut: PairJrUtilStopsWithStops
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun physicalStopTemplate(
        name: String,
        externalId: String,
        relationalId: Long? = null,
        position: Coordinate = Coordinate(0.0, 0.0),
    ) = PhysicalStop(
        relationalId = relationalId,
        name = name,
        externalId = externalId,
        position = geometryFactory.createPoint(position),
        tags = emptyMap(),
    )

    @BeforeEach
    fun setup() {
        mockLineVersionJpaRepository = mock()
        mockStopJpaRepository = mock()
        mockNormalizeStopName = mock {
            on { normalize(any()) } doAnswer { invocation -> invocation.getArgument(0) }
        }
        sut = PairJrUtilStopsWithStops(
            mockLineVersionJpaRepository,
            mockStopJpaRepository,
        )
    }

    private fun pairingsMatches(
        stops: List<StopPositionEnrichmentDto>,
        parsedStops: List<PhysicalStop>,
        mappings: List<Int>,
        pairingsForLine: Map<Long, PhysicalStop>,
    ) {
        val matchingExternalIds = mappings.map { parsedStops[it].externalId }
        for ((stop, physicalStopExternalId) in stops.zip(matchingExternalIds)) {
            assertContains(pairingsForLine, stop.relationalId)
            val physicalStop = pairingsForLine[stop.relationalId]
            assertNotNull(physicalStop)
            assertEquals(physicalStopExternalId, physicalStop.externalId)
        }
    }

    @Test
    fun `should match stops for one line with same names`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A"),
            StopPositionEnrichmentDto(2, "B"),
            StopPositionEnrichmentDto(3, "C"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("A", "EA"),
            physicalStopTemplate("C", "EC"),
            physicalStopTemplate("B", "EB"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        for (lineHints in listOf(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops)), listOf())) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 1)
            val pairingsForLine = result.first()
            assertEquals(linePublicCode, pairingsForLine.linePublicCode)
            pairingsMatches(stops, parsedStops, listOf(0, 2, 1), pairingsForLine.assignmentsByStopId)
        }
    }

    @Test
    fun `should match stops for one line with same names by filling missing stops with global search`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A"),
            StopPositionEnrichmentDto(2, "B"),
            StopPositionEnrichmentDto(3, "C"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("A", "EA"),
            physicalStopTemplate("C", "EC"),
            physicalStopTemplate("B", "EB"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        val result = sut.pairJrUtilStopsWithStops(
            JrUtilGtfsParseResult(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops.drop(1))), parsedStops),
            mockNormalizeStopName,
        )

        assert(result.size == 1)
        val pairingsForLine = result.first()
        assertEquals(linePublicCode, pairingsForLine.linePublicCode)
        pairingsMatches(stops, parsedStops, listOf(0, 2, 1), pairingsForLine.assignmentsByStopId)
    }

    @Test
    fun `should match stops for one line with same names where names overlap`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A"),
            StopPositionEnrichmentDto(2, "A,B"),
            StopPositionEnrichmentDto(3, "A,C"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("A,B", "EB"),
            physicalStopTemplate("A", "EA"),
            physicalStopTemplate("A,C", "EC"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        for (lineHints in listOf(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops)), listOf())) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 1)
            val pairingsForLine = result.first()
            assertEquals(linePublicCode, pairingsForLine.linePublicCode)
            pairingsMatches(stops, parsedStops, listOf(1, 0, 2), pairingsForLine.assignmentsByStopId)
        }
    }

    @Test
    fun `should match stops for one line with similar names`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A"),
            StopPositionEnrichmentDto(2, "B"),
            StopPositionEnrichmentDto(3, "C"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("Loc,A", "EA"),
            physicalStopTemplate("Loc,C", "EC"),
            physicalStopTemplate("Loc,B", "EB"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        for (lineHints in listOf(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops)), listOf())) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 1)
            val pairingsForLine = result.first()
            assertEquals(linePublicCode, pairingsForLine.linePublicCode)
            pairingsMatches(stops, parsedStops, listOf(0, 2, 1), pairingsForLine.assignmentsByStopId)
        }
    }

    @Test
    fun `should match stops for one line with not so similar names`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A,Stop"),
            StopPositionEnrichmentDto(2, "B,Stop"),
            StopPositionEnrichmentDto(3, "C,Stop"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("Loc,A Stop", "EA"),
            physicalStopTemplate("Loc,C Stop", "EC"),
            physicalStopTemplate("Loc,B Stop", "EB"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        for (lineHints in listOf(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops)), listOf())) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 1)
            val pairingsForLine = result.first()
            assertEquals(linePublicCode, pairingsForLine.linePublicCode)
            pairingsMatches(stops, parsedStops, listOf(0, 2, 1), pairingsForLine.assignmentsByStopId)
        }
    }

    @Test
    fun `should match stops for one line with similar names with extra parsed stops`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A,Stop"),
            StopPositionEnrichmentDto(2, "B,Stop"),
            StopPositionEnrichmentDto(3, "C,Stop"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("Loc,A,Stop", "EA"),
            physicalStopTemplate("Loc,C,Stop", "EC"),
            physicalStopTemplate("Loc,B,Stop", "EB"),
        )
        val extraParsedStops = listOf(
            physicalStopTemplate("Loc,E,Stop", "EE"),
            physicalStopTemplate("Loc,D,Stop", "ED"),
            physicalStopTemplate("Loc,F,Stop", "EF"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        for (lineHints in listOf(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops + extraParsedStops)), listOf())) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops + extraParsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 1)
            val pairingsForLine = result.first()
            assertEquals(linePublicCode, pairingsForLine.linePublicCode)
            pairingsMatches(stops, parsedStops, listOf(0, 2, 1), pairingsForLine.assignmentsByStopId)
        }
    }

    @Test
    fun `should match stops for one line with similar names by filling context by matched stops`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "Loc,A,Stop"),
            StopPositionEnrichmentDto(2, "B,Stop"),
            StopPositionEnrichmentDto(3, "C,Stop"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("Loc,B,Stop", "EBL"),
            physicalStopTemplate("Loc,A,Stop", "EAL"),
            physicalStopTemplate("Loc,C,Stop", "ECL"),
        )
        val extraParsedStops = listOf(
            physicalStopTemplate("Place,C,Stop", "ECP"),
            physicalStopTemplate("Place,A,Stop", "EAP"),
            physicalStopTemplate("Place,B,Stop", "EBP"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        val result = sut.pairJrUtilStopsWithStops(
            JrUtilGtfsParseResult(listOf(), parsedStops + extraParsedStops),
            mockNormalizeStopName,
        )

        assert(result.size == 1)
        val pairingsForLine = result.first()
        assertEquals(linePublicCode, pairingsForLine.linePublicCode)
        pairingsMatches(stops, parsedStops, listOf(1, 0, 2), pairingsForLine.assignmentsByStopId)
    }

    @Test
    fun `should match stops for one line with same names and pick closer stop when multiple stops match`() {
        val linePublicCode = "ABC"
        val stops = listOf(
            StopPositionEnrichmentDto(1, "A"),
            StopPositionEnrichmentDto(2, "B"),
            StopPositionEnrichmentDto(3, "C"),
        )
        val parsedStops = listOf(
            physicalStopTemplate("B", "EB1", position = Coordinate(14.1, 50.1)),
            physicalStopTemplate("A", "EA1", position = Coordinate(14.12, 50.0)),
            physicalStopTemplate("C", "EC1", position = Coordinate(0.0, 0.0)),
        )
        val extraParsedStops = listOf(
            physicalStopTemplate("A", "EA2", position = Coordinate(14.12, 49.0)),
            physicalStopTemplate("A", "EA3", position = Coordinate(0.0, 0.0)),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn listOf(linePublicCode)
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(linePublicCode) } doReturn stops

        for (lineHints in listOf(listOf(JrUtilGtfsLineParseResult(linePublicCode, parsedStops + extraParsedStops)), listOf())) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops + extraParsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 1)
            val pairingsForLine = result.first()
            assertEquals(linePublicCode, pairingsForLine.linePublicCode)
            pairingsMatches(stops, parsedStops, listOf(1, 0, 2), pairingsForLine.assignmentsByStopId)
        }
    }

    @Test
    fun `should match stops for multiple lines`() {
        val lines = listOf(
            Triple("ABC", listOf(1, 0, 2), listOf(
                StopPositionEnrichmentDto(1, "A"),
                StopPositionEnrichmentDto(2, "B"),
                StopPositionEnrichmentDto(3, "C"),
            )),
            Triple("DBA", listOf(4, 0, 1), listOf(
                StopPositionEnrichmentDto(4, "Town,D"),
                StopPositionEnrichmentDto(2, "Town,B"),
                StopPositionEnrichmentDto(1, "Town,A"),
            )),
            Triple("EFA", listOf(5, 3, 1), listOf(
                StopPositionEnrichmentDto(5, "Town E"),
                StopPositionEnrichmentDto(6, "Town F"),
                StopPositionEnrichmentDto(1, "Town A"),
            )),
        )
        val parsedStops = listOf(
            physicalStopTemplate("Town,B", "EB"),
            physicalStopTemplate("Town,A", "EA"),
            physicalStopTemplate("Town,C", "EC"),
            physicalStopTemplate("Town,F", "EF"),
            physicalStopTemplate("Town,D", "ED"),
            physicalStopTemplate("Town,E", "EE"),
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn lines.map { it.first }
        whenever { mockStopJpaRepository.findAllPositionEnrichmentDtoByLinePublicCode(any()) } doAnswer { invocation ->
            val linePublicCode = invocation.getArgument<String>(0)
            lines.find { it.first == linePublicCode }?.third ?: listOf()
        }

        for (lineHints in listOf(
            lines.map { JrUtilGtfsLineParseResult(it.first, it.second.map { idx -> parsedStops[idx] }) },
            lines.drop(1).map {
                JrUtilGtfsLineParseResult(it.first, it.second.drop(1).map { idx -> parsedStops[idx] })
            },
            listOf(),
        )) {
            val result = sut.pairJrUtilStopsWithStops(
                JrUtilGtfsParseResult(lineHints, parsedStops),
                mockNormalizeStopName,
            )

            assert(result.size == 3)
            for ((idx, pairings) in result.withIndex()) {
                assertEquals(lines[idx].first, pairings.linePublicCode)
                pairingsMatches(lines[idx].third, parsedStops, lines[idx].second, pairings.assignmentsByStopId)
            }
        }
    }
}
