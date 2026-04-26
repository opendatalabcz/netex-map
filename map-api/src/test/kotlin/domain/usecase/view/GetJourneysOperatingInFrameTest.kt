package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.JourneyFrameDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class GetJourneysOperatingInFrameTest {
    private lateinit var mockJourneyJpaRepository: JourneyJpaRepository
    private lateinit var mockScheduledStopJpaRepository: ScheduledStopJpaRepository
    private lateinit var mockLineVersionJpaRepository: LineVersionJpaRepository
    private lateinit var mockLevelOfDetailUseCase: LevelOfDetailUseCase
    private lateinit var mockGetEncodedRoutesUseCase: GetEncodedRoutesUseCase
    private lateinit var sut: GetJourneysOperatingInFrame

    private fun journeyTemplate(
        nextDayFirstStopIndex: Int?,
    ) = JourneyFrameDto(
        relationalId = 1L,
        lineVersionId = 1L,
        routeId = 1L,
        nextDayFirstStopIndex = nextDayFirstStopIndex,
        timezone = "UTC",
    )

    @BeforeEach
    fun setup() {
        mockJourneyJpaRepository = mock()
        mockScheduledStopJpaRepository = mock()
        mockLineVersionJpaRepository = mock {
            on { findAllFrameDtoByLineVersionIds(any()) } doReturn emptyList()
        }
        mockLevelOfDetailUseCase = mock {
            on { getMinRouteLength(any()) } doReturn 1.0
            on { getVisibleLineTypes(any()) } doReturn listOf("B")
        }
        mockGetEncodedRoutesUseCase = mock {
            on { getEncodedRoutes(any()) } doReturn emptyList()
        }
        sut = GetJourneysOperatingInFrame(
            mockJourneyJpaRepository,
            mockScheduledStopJpaRepository,
            mockLineVersionJpaRepository,
            mockLevelOfDetailUseCase,
            mockGetEncodedRoutesUseCase,
        )
    }

    @Test
    fun `should assign current date to scheduled stops when nextDayFirstStopIndex is null`() {
        val journey = journeyTemplate(null)
        val scheduledStops = listOf(
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 0,
                arrival = null,
                departure = LocalTime.of(9,50),
            ),
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 1,
                arrival = LocalTime.of(9,57),
                departure = LocalTime.of(9,57),
            ),
        )
        val inputDateTime = OffsetDateTime.of(2025, 1, 1, 9, 47, 0, 0, ZoneOffset.UTC)

        whenever { mockJourneyJpaRepository.findAllFrameDtoOperatingInFrame(
            any(), any(), any(), any(), any(), any(), any()
        ) } doReturn listOf(journey)
        whenever { mockJourneyJpaRepository.findAllFrameDtoOperatingInFrameWithNextDayOperation(
            any(), any(), any(), any(), any(), any(), any()
        ) } doReturn emptyList()
        whenever { mockScheduledStopJpaRepository.findAllDtoByJourneyIds(any()) } doReturn scheduledStops

        val result = sut.getJourneysOperatingInFrame(
            0.0, 0.0, 0.0, 0.0, 1, inputDateTime, emptySet(), emptySet(), emptySet()
        )

        assertEquals(1, result.journeys.size)
        val resultJourney = result.journeys.first()
        assertEquals(2, resultJourney.schedule.size)
        val resultSchedule = resultJourney.schedule
        assertEquals(OffsetDateTime.of(inputDateTime.toLocalDate(), scheduledStops[0].departure, ZoneOffset.UTC), resultSchedule[0].departure)
        assertNull(resultSchedule[0].arrival)
        assertEquals(OffsetDateTime.of(inputDateTime.toLocalDate(), scheduledStops[1].departure, ZoneOffset.UTC), resultSchedule[1].departure)
        assertNull(resultSchedule[1].arrival)
    }

    @Test
    fun `should assign next day date to scheduled stops after or at nextDayFirstStopIndex`() {
        val journey = journeyTemplate(1)
        val scheduledStops = listOf(
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 0,
                arrival = null,
                departure = LocalTime.of(9,50),
            ),
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 1,
                arrival = LocalTime.of(1,57),
                departure = LocalTime.of(1,57),
            ),
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 2,
                arrival = LocalTime.of(2,12),
                departure = null,
            ),
        )
        val inputDateTime = OffsetDateTime.of(2025, 1, 1, 9, 47, 0, 0, ZoneOffset.UTC)
        val nextDayDateTime = OffsetDateTime.of(2025, 1, 2, 9, 47, 0, 0, ZoneOffset.UTC)

        whenever { mockJourneyJpaRepository.findAllFrameDtoOperatingInFrame(
            any(), any(), any(), any(), any(), any(), any()
        ) } doReturn listOf(journey)
        whenever { mockJourneyJpaRepository.findAllFrameDtoOperatingInFrameWithNextDayOperation(
            any(), any(), any(), any(), any(), any(), any()
        ) } doReturn emptyList()
        whenever { mockScheduledStopJpaRepository.findAllDtoByJourneyIds(any()) } doReturn scheduledStops

        val result = sut.getJourneysOperatingInFrame(
            0.0, 0.0, 0.0, 0.0, 1, inputDateTime, emptySet(), emptySet(), emptySet()
        )

        assertEquals(1, result.journeys.size)
        val resultJourney = result.journeys.first()
        assertEquals(3, resultJourney.schedule.size)
        val resultSchedule = resultJourney.schedule
        assertEquals(OffsetDateTime.of(inputDateTime.toLocalDate(), scheduledStops[0].departure, ZoneOffset.UTC), resultSchedule[0].departure)
        assertNull(resultSchedule[0].arrival)
        assertEquals(OffsetDateTime.of(nextDayDateTime.toLocalDate(), scheduledStops[1].departure, ZoneOffset.UTC), resultSchedule[1].departure)
        assertNull(resultSchedule[1].arrival)
        assertEquals(OffsetDateTime.of(nextDayDateTime.toLocalDate(), scheduledStops[2].arrival, ZoneOffset.UTC), resultSchedule[2].arrival)
        assertNull(resultSchedule[2].departure)
    }

    @Test
    fun `should assign next day date to scheduled stop's departure when departure is earlier than arrival`() {
        val journey = journeyTemplate(2)
        val scheduledStops = listOf(
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 0,
                arrival = null,
                departure = LocalTime.of(9,50),
            ),
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 1,
                arrival = LocalTime.of(23,57),
                departure = LocalTime.of(1,57),
            ),
            ScheduledStopDto(
                journeyId = journey.relationalId,
                stopOrder = 2,
                arrival = LocalTime.of(2,12),
                departure = null,
            ),
        )
        val inputDateTime = OffsetDateTime.of(2025, 1, 1, 9, 47, 0, 0, ZoneOffset.UTC)
        val nextDayDateTime = OffsetDateTime.of(2025, 1, 2, 9, 47, 0, 0, ZoneOffset.UTC)

        whenever { mockJourneyJpaRepository.findAllFrameDtoOperatingInFrame(
            any(), any(), any(), any(), any(), any(), any()
        ) } doReturn listOf(journey)
        whenever { mockJourneyJpaRepository.findAllFrameDtoOperatingInFrameWithNextDayOperation(
            any(), any(), any(), any(), any(), any(), any()
        ) } doReturn emptyList()
        whenever { mockScheduledStopJpaRepository.findAllDtoByJourneyIds(any()) } doReturn scheduledStops

        val result = sut.getJourneysOperatingInFrame(
            0.0, 0.0, 0.0, 0.0, 1, inputDateTime, emptySet(), emptySet(), emptySet()
        )

        assertEquals(1, result.journeys.size)
        val resultJourney = result.journeys.first()
        assertEquals(3, resultJourney.schedule.size)
        val resultSchedule = resultJourney.schedule
        assertEquals(OffsetDateTime.of(inputDateTime.toLocalDate(), scheduledStops[0].departure, ZoneOffset.UTC), resultSchedule[0].departure)
        assertNull(resultSchedule[0].arrival)
        assertEquals(OffsetDateTime.of(inputDateTime.toLocalDate(), scheduledStops[1].arrival, ZoneOffset.UTC), resultSchedule[1].arrival)
        assertEquals(OffsetDateTime.of(nextDayDateTime.toLocalDate(), scheduledStops[1].departure, ZoneOffset.UTC), resultSchedule[1].departure)
        assertEquals(OffsetDateTime.of(nextDayDateTime.toLocalDate(), scheduledStops[2].arrival, ZoneOffset.UTC), resultSchedule[2].arrival)
        assertNull(resultSchedule[2].departure)
    }
}
