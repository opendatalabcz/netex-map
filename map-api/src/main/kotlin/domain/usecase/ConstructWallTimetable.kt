package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.repository.ActivePeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.OperatingPeriodWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.ScheduledStopWallDto
import cz.cvut.fit.gaierda1.domain.usecase.ConstructWallTimetableUseCase.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ConstructWallTimetable(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val activePeriodJpaRepository: ActivePeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
): ConstructWallTimetableUseCase {
    private fun reconstructWallOperatingPeriod(
        operatingPeriod: OperatingPeriodWallDto,
    ): Pair<WallOperatingDays, Map<WallOperationExceptionType, List<LocalDate>>> {
        // Scan for day type occurrences
        val dayTypeCounter = Array(7) { 0 }
        var currentDayOfWeekValue = operatingPeriod.fromDate.dayOfWeek.value - 1
        for (i in 0 until operatingPeriod.validDays.size) {
            if (operatingPeriod.validDays[i]) {
                dayTypeCounter[currentDayOfWeekValue] += 1
            }
            currentDayOfWeekValue = (currentDayOfWeekValue + 1) % 7
        }

        // Group by occurrence count (occurrence count, number of days with that occurrence count)
        // and find the most common occurrence count
        val mostCommonOccurrenceCount = dayTypeCounter
            .filter { it != 0 }
            .groupBy { it }
            .mapValues { (_, v) -> v.size }
            .maxBy { it.value }
            .key

        // Find common operating days
        val commonDays = mutableListOf<Int>()
        for ((idx, count) in dayTypeCounter.withIndex()) {
            if (count == 0) continue
            val exceptionCountByIncluding = mostCommonOccurrenceCount - count
            if (exceptionCountByIncluding <= count) {
                commonDays.add(idx)
            }
        }

        // Find operating exceptions
        val operatingExceptions = mutableMapOf<WallOperationExceptionType, MutableList<LocalDate>>(
            WallOperationExceptionType.ALSO_OPERATES to mutableListOf(),
            WallOperationExceptionType.DOES_NOT_OPERATE to mutableListOf(),
        )
        currentDayOfWeekValue = operatingPeriod.fromDate.dayOfWeek.value - 1
        val firstDate = operatingPeriod.fromDate.toLocalDate()
        for (i in 0 until operatingPeriod.validDays.size) {
            val currentDayOfWeekIsCommon = commonDays.contains(currentDayOfWeekValue)
            val operatesThisDay = operatingPeriod.validDays[i]
            if (operatesThisDay xor currentDayOfWeekIsCommon) {
                val exceptionType = if (operatesThisDay) WallOperationExceptionType.DOES_NOT_OPERATE
                                    else WallOperationExceptionType.ALSO_OPERATES
                operatingExceptions[exceptionType]!!.add(firstDate.plusDays(i.toLong()))
            }
            currentDayOfWeekValue = (currentDayOfWeekValue + 1) % 7
        }

        return WallOperatingDays(
            monday = commonDays.contains(0),
            tuesday = commonDays.contains(1),
            wednesday = commonDays.contains(2),
            thursday = commonDays.contains(3),
            friday = commonDays.contains(4),
            saturday = commonDays.contains(5),
            sunday = commonDays.contains(6),
        ) to operatingExceptions.filterValues { it.isNotEmpty() }
    }

    @Transactional(readOnly = true)
    override fun constructWallTimetable(lineVersionId: Long): WallTimetable? {
        val lineVersion = lineVersionJpaRepository.findWallDtoById(lineVersionId).orElse(null) ?: return null
        val activePeriods = activePeriodJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val journeys = journeyJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val operatingPeriods = operatingPeriodJpaRepository.findAllWallDtoByJourneyIds(
            journeys.map(JourneyWallDto::relationalId)
        )
        val scheduledStops = scheduledStopJpaRepository
            .findAllWallDtoByJourneyIds(journeys.map(JourneyWallDto::relationalId))
            .groupBy(ScheduledStopWallDto::journeyId)
            .mapValues { (_, routeStops) -> routeStops
                .sortedBy(ScheduledStopWallDto::stopOrder)
                .map { WallScheduledStop(
                    name = it.name,
                    stopOnRequest = it.stopOnRequest,
                    arrival = if (it.arrival == it.departure) null else it.arrival,
                    departure = it.departure,
                ) }
            }

        val journeysByOperatingPeriod = journeys
            .groupBy(JourneyWallDto::operatingPeriodId)
            .mapValues { (_, journeys) ->
                journeys.associate { journey ->
                    journey.relationalId to scheduledStops[journey.relationalId]!!
                }
            }

        val reconstructedOperatingPeriods = operatingPeriods.map { wallDto ->
            reconstructWallOperatingPeriod(wallDto)
                .let { operatingPeriodPair ->
                    WallOperatingPeriod(
                        operatingPeriodPair.first,
                        operatingPeriodPair.second,
                        journeysByOperatingPeriod[wallDto.relationalId]!!
                    )
                }
        }

        val reconstructedLineVersion = WallLineVersion(
            relationalId = lineVersion.relationalId,
            publicCode = lineVersion.publicCode,
            name = lineVersion.name,
            shortName = lineVersion.shortName,
            transportMode = lineVersion.transportMode,
            isDetour = lineVersion.isDetour,
            activePeriods = activePeriods,
        )

        return WallTimetable(
            lineVersion = reconstructedLineVersion,
            operatingPeriods = reconstructedOperatingPeriods,
        )
    }
}