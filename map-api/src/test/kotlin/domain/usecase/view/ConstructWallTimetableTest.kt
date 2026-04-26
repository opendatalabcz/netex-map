package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.ActivePeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatorJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.TariffStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.WithinRegionTransportBanJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatingPeriodDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatorDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyPatternStopWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyPatternWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyWallDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class ConstructWallTimetableTest {
    private lateinit var mockLineVersionJpaRepository: LineVersionJpaRepository
    private lateinit var mockActivePeriodJpaRepository: ActivePeriodJpaRepository
    private lateinit var mockJourneyJpaRepository: JourneyJpaRepository
    private lateinit var mockOperatingPeriodJpaRepository: OperatingPeriodJpaRepository
    private lateinit var mockScheduledStopJpaRepository: ScheduledStopJpaRepository
    private lateinit var mockOperatorJpaRepository: OperatorJpaRepository
    private lateinit var mockTariffStopJpaRepository: TariffStopJpaRepository
    private lateinit var mockStopJpaRepository: StopJpaRepository
    private lateinit var mockJourneyPatternJpaRepository: JourneyPatternJpaRepository
    private lateinit var mockJourneyPatternStopJpaRepository: JourneyPatternStopJpaRepository
    private lateinit var mockWithinRegionTransportBanJpaRepository: WithinRegionTransportBanJpaRepository
    private lateinit var sut: ConstructWallTimetable

    private fun journeyTemplate() = JourneyWallDto(
        relationalId = 1L,
        patternNumber = 0,
        operatingPeriodId = 1L,
        requiresOrdering = false,
        baggageStorage = false,
        cyclesAllowed = false,
        lowFloorAccess = false,
        reservationCompulsory = false,
        reservationPossible = false,
        snacksOnBoard = false,
        unaccompaniedMinorAssistance = false,
    )
    
    private fun scheduledStopTemplate() = ScheduledStopDto(
        journeyId = 1L,
        stopOrder = 0,
        arrival = LocalTime.of(10, 5),
        departure = LocalTime.of(11, 5),
    )
    
    private fun journeyPatternTemplate() = JourneyPatternWallDto(
        patternNumber = 0,
        direction = "O",
        routeId = null,
    )
    
    private fun journeyPatternStopTemplate() = JourneyPatternStopWallDto(
        patternNumber = 0,
        stopOrder = 0,
        tariffOrder = 0,
        distanceToNextStop = 1.0,
        forBoarding = false,
        forAlighting = false,
        requiresOrdering = false,
        stopOnRequest = false,
    )
    
    private fun lineVersionTemplate() = LineVersionDto(
        relationalId = 1L,
        publicCode = "ABC",
        name = "ABC",
        shortName = "ABC",
        transportMode = "B",
        lineType = "A",
        isDetour = false,
        validFrom = Instant.parse("2023-01-01T00:00:00Z"),
        validTo = Instant.parse("2023-12-31T23:59:59Z"),
        operatorId = 1L,
    )

    private fun operatorTemplate() = OperatorDto(
        relationalId = 1L,
        publicCode = "123",
        legalName = "",
        phone = "",
        email = "",
        url = "",
        addressLine = "",
    )
    
    @BeforeEach
    fun setup() {
        mockOperatingPeriodJpaRepository = mock()
        mockLineVersionJpaRepository = mock {
            on { findDtoById(any()) } doReturn Optional.of(lineVersionTemplate())
        }
        mockActivePeriodJpaRepository = mock {
            on { findAllDtoForSingleLineByLineVersionId(any()) } doReturn emptyList()
        }
        mockJourneyJpaRepository = mock {
            on { findAllWallDtoByLineVersionId(any()) } doReturn listOf(journeyTemplate())
        }
        mockScheduledStopJpaRepository = mock {
            on { findAllDtoByJourneyIds(any()) } doReturn listOf(scheduledStopTemplate())
        }
        mockOperatorJpaRepository = mock {
            on { findDtoByOperatorId(any()) } doReturn Optional.of(operatorTemplate())
        }
        mockTariffStopJpaRepository = mock {
            on { findAllDtoForSingleLineByLineVersionId(any()) } doReturn emptyList()
        }
        mockStopJpaRepository = mock {
            on { findAllDtoByStopIds(any()) } doReturn emptyList()
        }
        mockJourneyPatternJpaRepository = mock {
            on { findAllWallDtoByLineVersionId(any()) } doReturn listOf(journeyPatternTemplate())
        }
        mockJourneyPatternStopJpaRepository = mock {
            on { findAllWallDtoByLineVersionId(any()) } doReturn listOf(journeyPatternStopTemplate())
        }
        mockWithinRegionTransportBanJpaRepository = mock {
            on { findAllWallDtoByLineVersionId(any()) } doReturn emptyList()
        }
        sut = ConstructWallTimetable(
            mockLineVersionJpaRepository,
            mockActivePeriodJpaRepository,
            mockJourneyJpaRepository,
            mockOperatingPeriodJpaRepository,
            mockScheduledStopJpaRepository,
            mockOperatorJpaRepository,
            mockTariffStopJpaRepository,
            mockStopJpaRepository,
            mockJourneyPatternJpaRepository,
            mockJourneyPatternStopJpaRepository,
            mockWithinRegionTransportBanJpaRepository,
        )
    }

    @Test
    fun `should mark all days of week as regular when all days are valid`() {
        val days = 50
        val fromDate = LocalDateTime.of(2025, 1, 1, 0, 0)
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { true },
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(true, resultOperatingPeriod.operatingDays.monday)
        assertEquals(true, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(true, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(true, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(true, resultOperatingPeriod.operatingDays.friday)
        assertEquals(true, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(true, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(0, resultOperatingPeriod.operationExceptions.size)
    }

    @Test
    fun `should not mark any day of week as regular when all days are not valid`() {
        val days = 50
        val fromDate = LocalDateTime.of(2025, 1, 1, 0, 0)
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { false },
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(false, resultOperatingPeriod.operatingDays.monday)
        assertEquals(false, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(false, resultOperatingPeriod.operatingDays.friday)
        assertEquals(false, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(false, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(0, resultOperatingPeriod.operationExceptions.size)
    }

    @Test
    fun `should mark only monday as regular when only mondays are valid`() {
        val days = 50
        val fromDate = /* Monday */ LocalDateTime.of(2025, 1, 6, 0, 0)
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { it % 7 == 0 },
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(true, resultOperatingPeriod.operatingDays.monday)
        assertEquals(false, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(false, resultOperatingPeriod.operatingDays.friday)
        assertEquals(false, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(false, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(0, resultOperatingPeriod.operationExceptions.size)
    }

    @Test
    fun `should mark only weekend days as regular when only weekend days are valid`() {
        val days = 50
        val fromDate = /* Monday */ LocalDateTime.of(2025, 1, 6, 0, 0)
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { it % 7 == 5 || it % 7 == 6 },
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(false, resultOperatingPeriod.operatingDays.monday)
        assertEquals(false, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(false, resultOperatingPeriod.operatingDays.friday)
        assertEquals(true, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(true, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(0, resultOperatingPeriod.operationExceptions.size)
    }

    @Test
    fun `should mark an exception from regular operation`() {
        val days = 50
        val fromDate = /* Monday */ LocalDateTime.of(2025, 1, 6, 0, 0)
        val exceptionDayOffset = 14
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { it % 7 == 0  && it != exceptionDayOffset},
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(true, resultOperatingPeriod.operatingDays.monday)
        assertEquals(false, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(false, resultOperatingPeriod.operatingDays.friday)
        assertEquals(false, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(false, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(1, resultOperatingPeriod.operationExceptions.size)
        val doesNotOperateExceptions = resultOperatingPeriod.operationExceptions[ConstructWallTimetableUseCase.WallOperationExceptionType.DOES_NOT_OPERATE]
        assertNotNull(doesNotOperateExceptions)
        assertEquals(1, doesNotOperateExceptions.size)
        assertEquals(fromDate.plusDays(exceptionDayOffset.toLong()).toLocalDate(), doesNotOperateExceptions.first())
    }

    @Test
    fun `should mark an exception from non regular operation`() {
        val days = 50
        val fromDate = /* Monday */ LocalDateTime.of(2025, 1, 6, 0, 0)
        val exceptionDayOffset = 3
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { it % 7 == 0 || it == exceptionDayOffset},
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(true, resultOperatingPeriod.operatingDays.monday)
        assertEquals(false, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(false, resultOperatingPeriod.operatingDays.friday)
        assertEquals(false, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(false, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(1, resultOperatingPeriod.operationExceptions.size)
        val doesNotOperateExceptions = resultOperatingPeriod.operationExceptions[ConstructWallTimetableUseCase.WallOperationExceptionType.ALSO_OPERATES]
        assertNotNull(doesNotOperateExceptions)
        assertEquals(1, doesNotOperateExceptions.size)
        assertEquals(fromDate.plusDays(exceptionDayOffset.toLong()).toLocalDate(), doesNotOperateExceptions.first())
    }

    @Test
    fun `should mark a day as regular when it operates same amount of days as it doesn't`() {
        val days = 24
        val fromDate = /* Monday */ LocalDateTime.of(2025, 1, 6, 0, 0)
        val exceptionDaysOffset = 10
        val operatingPeriod = OperatingPeriodDto(
            relationalId = 1L,
            fromDate = fromDate,
            toDate = fromDate.plusDays(days.toLong() - 1),
            validDays = Array(days) { it % 7 == 0 && it < exceptionDaysOffset},
        )

        whenever { mockOperatingPeriodJpaRepository.findAllWallDtoByIds(any()) } doReturn listOf(operatingPeriod)

        val result = sut.constructWallTimetable(1L)

        assertNotNull(result)
        assertEquals(1, result.operatingPeriods.size)
        val resultOperatingPeriod = result.operatingPeriods.first()
        assertEquals(true, resultOperatingPeriod.operatingDays.monday)
        assertEquals(false, resultOperatingPeriod.operatingDays.tuesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.wednesday)
        assertEquals(false, resultOperatingPeriod.operatingDays.thursday)
        assertEquals(false, resultOperatingPeriod.operatingDays.friday)
        assertEquals(false, resultOperatingPeriod.operatingDays.saturday)
        assertEquals(false, resultOperatingPeriod.operatingDays.sunday)
        assertEquals(1, resultOperatingPeriod.operationExceptions.size)
        val doesNotOperateExceptions = resultOperatingPeriod.operationExceptions[ConstructWallTimetableUseCase.WallOperationExceptionType.DOES_NOT_OPERATE]
        assertNotNull(doesNotOperateExceptions)
        assertEquals(2, doesNotOperateExceptions.size)
        assertEquals(fromDate.plusDays(14L).toLocalDate(), doesNotOperateExceptions[0])
        assertEquals(fromDate.plusDays(21L).toLocalDate(), doesNotOperateExceptions[1])
    }
}
