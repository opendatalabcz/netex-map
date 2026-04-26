package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.ActivePeriod
import cz.cvut.fit.gaierda1.data.orm.model.LineType
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.TransportMode
import cz.cvut.fit.gaierda1.data.orm.repository.ActivePeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime
import java.util.function.Consumer
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CalculateLineVersionActivePeriodsTest {
    private lateinit var mockLineVersionJpaRepository: LineVersionJpaRepository
    private lateinit var mockActivePeriodJpaRepository: ActivePeriodJpaRepository
    private lateinit var mockTransactionTemplate: TransactionTemplate
    private lateinit var sut: CalculateLineVersionActivePeriods
    private val pageSize: Int = 4
    private var savedActivePeriodsInTransaction = mutableListOf<MutableList<ActivePeriod>>()


    private fun lineVersionTemplate(
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
        publicCode: String,
        isDetour: Boolean,
        relationalId: Long? = null,
    ) = LineVersion(
        validFrom = validFrom,
        validTo = validTo,
        publicCode = publicCode,
        isDetour = isDetour,
        relationalId = relationalId,
        name = "Stop I -- Stop II",
        shortName = publicCode,
        transportMode = TransportMode.BUS,
        lineType = LineType.URBAN,
        operator = mock(),
        activePeriods = emptyList(),
        tariffStops = emptyList(),
    )

    @BeforeEach
    fun setup() {
        mockLineVersionJpaRepository = mock()
        mockActivePeriodJpaRepository = mock {
            on { deleteAll() }.doAnswer {}
            on { saveAll<ActivePeriod>(any()) } doAnswer { invocation ->
                val activePeriods = invocation.getArgument<Iterable<ActivePeriod>>(0)
                savedActivePeriodsInTransaction.last().addAll(activePeriods)
                activePeriods.toList()
            }
        }
        mockTransactionTemplate = mock {
            on { executeWithoutResult(any()) }.then { invocation ->
                savedActivePeriodsInTransaction.add(mutableListOf())
                val consumer = invocation.getArgument<Consumer<TransactionStatus>>(0)
                consumer.accept(mock())
            }
        }
        sut = CalculateLineVersionActivePeriods(
            mockLineVersionJpaRepository,
            mockActivePeriodJpaRepository,
            mockTransactionTemplate,
            pageSize,
        )
    }

    private fun ActivePeriod.assertMatches(lineVersionId: Long?, activeFrom: OffsetDateTime, activeTo: OffsetDateTime) {
        assertEquals(activeFrom, this.periodId.fromDate)
        assertEquals(activeTo, this.toDate)
        assertEquals(lineVersionId, this.periodId.lineVersionId)
    }

    @Test
    fun `should return single ActivePeriod for single LineVersion`() {
        val lineVersion = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "ABC",
            isDetour = false,
            relationalId = 10,
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn mutableListOf(lineVersion.publicCode)
        whenever { mockLineVersionJpaRepository.findAllByPublicCodes(any()) } doReturn listOf(lineVersion)

        sut.calculateActivePeriods()

        val savedActivePeriods = savedActivePeriodsInTransaction.flatMap { it }
        assertEquals(1, savedActivePeriods.size)
        savedActivePeriods[0]
            .assertMatches(lineVersion.relationalId, lineVersion.validFrom, lineVersion.validTo)
    }

    @Test
    fun `should return multiple ActivePeriods for different LineVersions`() {
        val lineVersion1 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "ABC",
            isDetour = false,
            relationalId = 1,
        )
        val lineVersion2 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "DEF",
            isDetour = false,
            relationalId = 2,
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn mutableListOf(lineVersion1.publicCode, lineVersion2.publicCode)
        whenever { mockLineVersionJpaRepository.findAllByPublicCodes(any()) } doAnswer { invocation ->
            val publicCodes = invocation.getArgument<List<String>>(0)
            listOf(lineVersion1, lineVersion2).filter { it.publicCode in publicCodes }
        }

        sut.calculateActivePeriods()

        val savedActivePeriods = savedActivePeriodsInTransaction.flatMap { it }
        assertEquals(2, savedActivePeriods.size)
        savedActivePeriods[0]
            .assertMatches(lineVersion1.relationalId, lineVersion1.validFrom, lineVersion1.validTo)
        savedActivePeriods[1]
            .assertMatches(lineVersion2.relationalId, lineVersion2.validFrom, lineVersion2.validTo)
    }

    @Test
    fun `should return neighbouring ActivePeriods for two overlapping LineVersions`() {
        val lineVersion1 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "ABC",
            isDetour = false,
            relationalId = 1,
        )
        val lineVersion2 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-15T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-02-12T23:59:59+00:00"),
            publicCode = lineVersion1.publicCode,
            isDetour = false,
            relationalId = 2,
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn mutableListOf(lineVersion1.publicCode)
        whenever { mockLineVersionJpaRepository.findAllByPublicCodes(eq(listOf(lineVersion1.publicCode))) } doReturn listOf(lineVersion1, lineVersion2)

        sut.calculateActivePeriods()

        val savedActivePeriods = savedActivePeriodsInTransaction.flatMap { it }
        assertEquals(2, savedActivePeriods.size)
        savedActivePeriods[0]
            .assertMatches(lineVersion1.relationalId, lineVersion1.validFrom, lineVersion2.validFrom)
        savedActivePeriods[1]
            .assertMatches(lineVersion2.relationalId, lineVersion2.validFrom, lineVersion2.validTo)
    }

    @Test
    fun `should return neighbouring ActivePeriods for two overlapping LineVersions with one contained in the other`() {
        val lineVersion1 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "ABC",
            isDetour = false,
            relationalId = 1,
        )
        val lineVersion2 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-15T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-22T23:59:59+00:00"),
            publicCode = lineVersion1.publicCode,
            isDetour = false,
            relationalId = 2,
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn mutableListOf(lineVersion1.publicCode)
        whenever { mockLineVersionJpaRepository.findAllByPublicCodes(eq(listOf(lineVersion1.publicCode))) } doReturn listOf(lineVersion1, lineVersion2)

        sut.calculateActivePeriods()

        val savedActivePeriods = savedActivePeriodsInTransaction.flatMap { it }
        assertEquals(3, savedActivePeriods.size)
        savedActivePeriods[0]
            .assertMatches(lineVersion1.relationalId, lineVersion1.validFrom, lineVersion2.validFrom)
        savedActivePeriods[1]
            .assertMatches(lineVersion2.relationalId, lineVersion2.validFrom, lineVersion2.validTo)
        savedActivePeriods[2]
            .assertMatches(lineVersion1.relationalId, lineVersion2.validTo, lineVersion1.validTo)
    }

    @Test
    fun `should return one ActivePeriod for two completely overlapping LineVersions with one being detour`() {
        val lineVersion1 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "ABC",
            isDetour = false,
            relationalId = 1,
        )
        val lineVersion2 = lineVersionTemplate(
            validFrom = lineVersion1.validFrom,
            validTo = lineVersion1.validTo,
            publicCode = lineVersion1.publicCode,
            isDetour = true,
            relationalId = 2,
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn mutableListOf(lineVersion1.publicCode)
        whenever { mockLineVersionJpaRepository.findAllByPublicCodes(eq(listOf(lineVersion1.publicCode))) } doReturn listOf(lineVersion1, lineVersion2)

        sut.calculateActivePeriods()

        val savedActivePeriods = savedActivePeriodsInTransaction.flatMap { it }
        assertEquals(1, savedActivePeriods.size)
        savedActivePeriods[0]
            .assertMatches(lineVersion2.relationalId, lineVersion1.validFrom, lineVersion1.validTo)
    }

    @Test
    fun `should return one ActivePeriod for two overlapping LineVersions starting at the same date with one being detour`() {
        val lineVersion1 = lineVersionTemplate(
            validFrom = OffsetDateTime.parse("2023-01-01T00:00:00+00:00"),
            validTo = OffsetDateTime.parse("2023-01-31T23:59:59+00:00"),
            publicCode = "ABC",
            isDetour = false,
            relationalId = 1,
        )
        val lineVersion2 = lineVersionTemplate(
            validFrom = lineVersion1.validFrom,
            validTo = OffsetDateTime.parse("2023-01-22T23:59:59+00:00"),
            publicCode = lineVersion1.publicCode,
            isDetour = true,
            relationalId = 2,
        )

        whenever { mockLineVersionJpaRepository.findAllPublicCodes() } doReturn mutableListOf(lineVersion1.publicCode)
        whenever { mockLineVersionJpaRepository.findAllByPublicCodes(eq(listOf(lineVersion1.publicCode))) } doReturn listOf(lineVersion1, lineVersion2)

        sut.calculateActivePeriods()

        val savedActivePeriods = savedActivePeriodsInTransaction.flatMap { it }
        assertEquals(2, savedActivePeriods.size)
        savedActivePeriods[0]
            .assertMatches(lineVersion2.relationalId, lineVersion1.validFrom, lineVersion2.validTo)
        savedActivePeriods[1]
            .assertMatches(lineVersion1.relationalId, lineVersion2.validTo, lineVersion1.validTo)

    }
}
