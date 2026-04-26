package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.*
import cz.cvut.fit.gaierda1.data.orm.repository.*
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.any as nullAny
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.io.InputStream
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ImportTimetablesTest {
    private lateinit var mockLineVersionJpaRepository: LineVersionJpaRepository
    private lateinit var mockTariffStopJpaRepository: TariffStopJpaRepository
    private lateinit var mockStopJpaRepository: StopJpaRepository
    private lateinit var mockOperatorJpaRepository: OperatorJpaRepository
    private lateinit var mockOperatingPeriodJpaRepository: OperatingPeriodJpaRepository
    private lateinit var mockJourneyJpaRepository: JourneyJpaRepository
    private lateinit var mockScheduledStopJpaRepository: ScheduledStopJpaRepository
    private lateinit var mockJourneyPatternJpaRepository: JourneyPatternJpaRepository
    private lateinit var mockJourneyPatternStopJpaRepository: JourneyPatternStopJpaRepository
    private lateinit var mockWithinRegionTransportBanJpaRepository: WithinRegionTransportBanJpaRepository
    private lateinit var mockTransactionTemplate: TransactionTemplate
    private lateinit var mockEntityManager: EntityManager
    private val batchSize = 6
    private lateinit var mockTimetableSource: TimetableSourcePort
    private lateinit var mockTimetableParser: TimetableParserPort
    private lateinit var mockNextDayOperationUseCase: CalculateNextDayOperationUseCase
    private lateinit var sut: ImportTimetables

    private fun stopTemplate(relationalId: Long?, linePublicCode: String) = Stop(
        relationalId = relationalId,
        name = "",
        linePublicCode = linePublicCode,
        bistro = false,
        borderCrossing = false,
        displaysForVisuallyImpaired = false,
        lowFloorAccess = false,
        parkAndRidePark = false,
        suitableForHeavilyDisabled = false,
        toilet = false,
        wheelChairAccessToilet = false,
        otherTransportModes = null,
    )

    private fun operatorTemplate(relationalId: Long?, publicCode: String) = Operator(
        relationalId = relationalId,
        publicCode = publicCode,
        legalName = "",
        phone = "",
        email = "",
        url = "",
        addressLine = "",
    )

    private fun tariffStopTemplate(lineVersion: LineVersion, stopPlace: Stop) = TariffStop(
        stopId = TariffStopId(lineVersion.relationalId, 0),
        lineVersion = lineVersion,
        tariffZone = "",
        stopPlace = stopPlace,
    )

    private fun lineVersionTemplate(relationalId: Long?, operator: Operator, publicCode: String) = LineVersion(
        relationalId = relationalId,
        publicCode = publicCode,
        name = "",
        shortName = "",
        transportMode = TransportMode.BUS,
        lineType = LineType.URBAN,
        isDetour = false,
        validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
        validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
        operator = operator,
        activePeriods = emptyList(),
        tariffStops = emptyList(),
    )

    private fun operatingPeriodTemplate(relationalId: Long?, validDays: List<Boolean>) = OperatingPeriod(
        relationalId = relationalId,
        fromDate = LocalDateTime.parse("2023-01-01T00:00:00"),
        toDate = LocalDateTime.parse("2023-01-31T23:59:59"),
        validDays = validDays,
    )

    private fun journeyPatternTemplate(lineVersion: LineVersion, patternNumber: Int) = JourneyPattern(
        patternId = JourneyPatternId(lineVersion.relationalId, patternNumber),
        lineVersion = lineVersion,
        direction = JourneyDirectionType.OUTBOUND,
        patternStops = emptyList(),
        transportBans = emptyList(),
        route = null,
    )

    private fun journeyPatterStopTemplate(journeyPattern: JourneyPattern, tariffStop: TariffStop) = JourneyPatternStop(
        stopId = JourneyPatternStopId(journeyPattern.patternId, 0),
        journeyPattern = journeyPattern,
        distanceToNextStop = 1.0,
        tariffOrder = tariffStop.stopId.tariffOrder,
        tariffStop = tariffStop,
        forBoarding = true,
        forAlighting = true,
        requiresOrdering = false,
        stopOnRequest = false,
    )

    private fun transportBanTemplate(journeyPatternStop: JourneyPatternStop) = WithinRegionTransportBan(
        banId = WithinRegionTransportBanId(journeyPatternStop.stopId, 0),
        journeyPatternStop = journeyPatternStop,
        journeyPattern = journeyPatternStop.journeyPattern,
    )

    private fun journeyTemplate(
        relationalId: Long?,
        journeyPattern: JourneyPattern,
        operatingPeriod: OperatingPeriod,
        journeyNumber: String,
    ) = Journey(
        relationalId = relationalId,
        journeyNumber = journeyNumber,
        schedule = emptyList(),
        operatingPeriod = operatingPeriod,
        journeyPattern = journeyPattern,
        requiresOrdering = false,
        baggageStorage = false,
        cyclesAllowed = false,
        lowFloorAccess = false,
        reservationCompulsory = false,
        reservationPossible = false,
        snacksOnBoard = false,
        unaccompaniedMinorAssistance = false,
        nextDayFirstStopIndex = null,
        beginTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        timezone = ZoneId.of("UTC"),
    )
    
    private fun scheduledStopTemplate(journey: Journey) = ScheduledStop(
        stopId = ScheduledStopId(journey.relationalId, 0),
        journey = journey,
        arrival = LocalTime.of(10, 0),
        departure = LocalTime.of(11, 0),
    )

    @BeforeEach
    fun setup() {
        mockTimetableSource = mock()
        mockTimetableParser = mock()
        mockNextDayOperationUseCase = mock {
            on { calculateFor(any()) }.then {}
        }
        mockLineVersionJpaRepository = mock {
            on { saveAll<LineVersion>(any()) } doAnswer { invocation ->
                    val elements = invocation.getArgument<Iterable<LineVersion>>(0)
                    elements.toList()
                }
            }
        mockTariffStopJpaRepository = mock {
            on { saveAll<TariffStop>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<TariffStop>>(0)
                elements.toList()
            }
        }
        mockStopJpaRepository = mock {
            on { saveAll<Stop>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<Stop>>(0)
                elements.toList()
            }
        }
        mockOperatorJpaRepository = mock {
            on { saveAll<Operator>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<Operator>>(0)
                elements.toList()
            }
        }
        mockOperatingPeriodJpaRepository = mock {
            on { saveAll<OperatingPeriod>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<OperatingPeriod>>(0)
                elements.toList()
            }
        }
        mockJourneyJpaRepository = mock {
            on { saveAll<Journey>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<Journey>>(0)
                elements.toList()
            }
        }
        mockScheduledStopJpaRepository = mock {
            on { saveAll<ScheduledStop>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<ScheduledStop>>(0)
                elements.toList()
            }
        }
        mockJourneyPatternJpaRepository = mock {
            on { saveAll<JourneyPattern>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<JourneyPattern>>(0)
                elements.toList()
            }
        }
        mockJourneyPatternStopJpaRepository = mock {
            on { saveAll<JourneyPatternStop>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<JourneyPatternStop>>(0)
                elements.toList()
            }
        }
        mockWithinRegionTransportBanJpaRepository = mock {
            on { saveAll<WithinRegionTransportBan>(any()) } doAnswer { invocation ->
                val elements = invocation.getArgument<Iterable<WithinRegionTransportBan>>(0)
                elements.toList()
            }
        }
        mockTransactionTemplate = mock {
            on { executeWithoutResult(any()) }.then { invocation ->
                val consumer = invocation.getArgument<Consumer<TransactionStatus>>(0)
                consumer.accept(mock())
            }
        }
        mockEntityManager = mock {
            on { getReference(eq(Operator::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(Stop::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(LineVersion::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(JourneyPattern::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(JourneyPatternStop::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(OperatingPeriod::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(Journey::class.java), nullAny()) } doReturn mock()
            on { getReference(eq(TariffStop::class.java), nullAny()) } doReturn mock()
        }
        sut = ImportTimetables(
            mockLineVersionJpaRepository,
            mockTariffStopJpaRepository,
            mockStopJpaRepository,
            mockOperatorJpaRepository,
            mockOperatingPeriodJpaRepository,
            mockJourneyJpaRepository,
            mockScheduledStopJpaRepository,
            mockJourneyPatternJpaRepository,
            mockJourneyPatternStopJpaRepository,
            mockWithinRegionTransportBanJpaRepository,
            mockTransactionTemplate,
            batchSize,
            mockEntityManager,
        )
    }

    @Test
    fun `should save only entities of new line versions`() {
        val inputStreams: Sequence<InputStream> = sequenceOf("0".byteInputStream(), "1".byteInputStream())

        val entities = listOf(null, 1L).map { id ->
            val res = TimetableParseResult()
            
            // LineVersion
            val operator = operatorTemplate(id, id.toString())
            res.addOperator(operator)
            val lineVersion = lineVersionTemplate(id, operator, id.toString())
            res.addLineVersion(lineVersion)
            val stops = listOf(
                stopTemplate(id, "1 $id"),
                stopTemplate(id?.let { it + 1 }, "2 " + id?.let { it + 1 }.toString()),
            )
            stops.forEach(res::addStop)
            val tariffStops = stops.map { tariffStopTemplate(lineVersion, it) }
            lineVersion.tariffStops = tariffStops
            
            // Journey
            val operatingPeriod = operatingPeriodTemplate(id, listOf(id == null))
            res.addOperatingPeriod(operatingPeriod)
            val journeyPattern = journeyPatternTemplate(lineVersion, if (id == null) 0 else 1)
            res.addJourneyPattern(journeyPattern)
            val journeyPatternStop = tariffStops.map { journeyPatterStopTemplate(journeyPattern, it) }
            journeyPattern.patternStops = journeyPatternStop
            val transportBans = journeyPatternStop.map { transportBanTemplate(it) }
            journeyPattern.transportBans = transportBans
            val journey = journeyTemplate(id, journeyPattern, operatingPeriod, id.toString())
            res.addJourney(journey)
            val schedule = journeyPatternStop.map { scheduledStopTemplate(journey) }
            journey.schedule = schedule
            
            return@map res
        }

        whenever { mockTimetableSource.provideInput() }.thenReturn(inputStreams)
        whenever { mockTimetableParser.parseTimetable(any(), any()) } doAnswer { invocation ->
            val idx = String(invocation.getArgument<InputStream>(0).readBytes()).toInt()
            val result = invocation.getArgument<TimetableParseResult?>(1) ?: TimetableParseResult()
            val resultAddition = entities[idx]
            for (e in resultAddition.journeyPatterns) result.addJourneyPattern(e)
            for (e in resultAddition.journeys) result.addJourney(e)
            for (e in resultAddition.lineVersions) result.addLineVersion(e)
            for (e in resultAddition.operatingPeriods) result.addOperatingPeriod(e)
            for (e in resultAddition.operators) result.addOperator(e)
            for (e in resultAddition.stops) result.addStop(e)
            result
        }

        sut.importTimetables(
            mockTimetableSource,
            mockTimetableParser,
            mockNextDayOperationUseCase,
        )

        // LineVersion
        argumentCaptor<Iterable<Operator>>().apply {
            verify(mockOperatorJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(1, savedEntitiesList.size)
            for (e in savedEntitiesList) assertNull(e.relationalId)
        }
        argumentCaptor<Iterable<LineVersion>>().apply {
            verify(mockLineVersionJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(1, savedEntitiesList.size)
            for (e in savedEntitiesList) assertNull(e.relationalId)
        }
        argumentCaptor<Iterable<Stop>>().apply {
            verify(mockStopJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(2, savedEntitiesList.size)
            for (e in savedEntitiesList) assertNull(e.relationalId)
        }
        argumentCaptor<Iterable<TariffStop>>().apply {
            verify(mockTariffStopJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(2, savedEntitiesList.size)
        }

        // Journey
        argumentCaptor<Iterable<OperatingPeriod>>().apply {
            verify(mockOperatingPeriodJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(1, savedEntitiesList.size)
            for (e in savedEntitiesList) assertNull(e.relationalId)
        }
        argumentCaptor<Iterable<JourneyPattern>>().apply {
            verify(mockJourneyPatternJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(1, savedEntitiesList.size)
        }
        argumentCaptor<Iterable<JourneyPatternStop>>().apply {
            verify(mockJourneyPatternStopJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(2, savedEntitiesList.size)
        }
        argumentCaptor<Iterable<WithinRegionTransportBan>>().apply {
            verify(mockWithinRegionTransportBanJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(2, savedEntitiesList.size)
        }
        argumentCaptor<Iterable<Journey>>().apply {
            verify(mockJourneyJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(1, savedEntitiesList.size)
            for (e in savedEntitiesList) assertNull(e.relationalId)
        }
        argumentCaptor<Iterable<ScheduledStop>>().apply {
            verify(mockScheduledStopJpaRepository).saveAll(capture())
            assertEquals(1, allValues.size)
            val savedEntitiesList = firstValue.toList()
            assertEquals(2, savedEntitiesList.size)
        }
    }
}
