package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStopId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class CalculateNextDayOperationTest {
    private lateinit var sut: CalculateNextDayOperation

    private fun journeyTemplate() = Journey(
        relationalId = null,
        journeyNumber = "123",
        schedule = listOf(),
        operatingPeriod = mock(),
        journeyPattern = mock(),
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
        endTime = LocalTime.of(12, 0),
        timezone = ZoneId.of("UTC")
    )

    @BeforeEach
    fun setup() {
        sut = CalculateNextDayOperation()
    }

    @Test
    fun `should not set nextDayFirstStopIndex when times are in correct order`() {
        val journey = journeyTemplate()
        journey.schedule = listOf(
            ScheduledStop(
                stopId = ScheduledStopId(null, 0),
                journey = journey,
                arrival = LocalTime.of(10, 0),
                departure = LocalTime.of(10, 5),
            ),
            ScheduledStop(
                stopId = ScheduledStopId(null, 1),
                journey = journey,
                arrival = LocalTime.of(11, 0),
                departure = LocalTime.of(11, 5),
            ),
            ScheduledStop(
                stopId = ScheduledStopId(null, 2),
                journey = journey,
                arrival = LocalTime.of(12, 0),
                departure = LocalTime.of(12, 5),
            )
        )

        sut.calculateFor(journey)

        assertNull(journey.nextDayFirstStopIndex)
    }

    @Test
    fun `should set nextDayFirstStopIndex when arrival time is earlier than previous departure time`() {
        val journey = journeyTemplate()
        journey.schedule = listOf(
            ScheduledStop(
                stopId = ScheduledStopId(null, 0),
                journey = journey,
                arrival = null,
                departure = LocalTime.of(10, 5),
            ),
            ScheduledStop(
                stopId = ScheduledStopId(null, 1),
                journey = journey,
                arrival = LocalTime.of(9, 0),
                departure = LocalTime.of(9, 5),
            ),
            ScheduledStop(
                stopId = ScheduledStopId(null, 2),
                journey = journey,
                arrival = LocalTime.of(12, 0),
                departure = null,
            )
        )

        sut.calculateFor(journey)

        assertEquals(1, journey.nextDayFirstStopIndex)
    }

    @Test
    fun `should set nextDayFirstStopIndex to the next stop when departure time is earlier`() {
        val journey = journeyTemplate()
        journey.schedule = listOf(
            ScheduledStop(
                stopId = ScheduledStopId(null, 0),
                journey = journey,
                arrival = null,
                departure = LocalTime.of(10, 5),
            ),
            ScheduledStop(
                stopId = ScheduledStopId(null, 1),
                journey = journey,
                arrival = LocalTime.of(23, 30),
                departure = LocalTime.of(0, 5),
            ),
            ScheduledStop(
                stopId = ScheduledStopId(null, 2),
                journey = journey,
                arrival = LocalTime.of(12, 0),
                departure = null,
            )
        )

        sut.calculateFor(journey)

        assertEquals(2, journey.nextDayFirstStopIndex)
    }
}
