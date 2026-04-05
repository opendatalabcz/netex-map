package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.ActivePeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.OperatingPeriodWallDto
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
    ): Pair<ConstructWallTimetableUseCase.WallOperatingDays, Map<ConstructWallTimetableUseCase.WallOperationExceptionType, List<LocalDate>>> {
        // Scan for day type occurrences
        val dayTypeOperatingCounter = Array(7) { 0 }
        val dayTypeNonOperatingCounter = Array(7) { 0 }
        var currentDayOfWeekValue = operatingPeriod.fromDate.dayOfWeek.value - 1
        for (i in 0 until operatingPeriod.validDays.size) {
            if (operatingPeriod.validDays[i]) {
                dayTypeOperatingCounter[currentDayOfWeekValue] += 1
            } else {
                dayTypeNonOperatingCounter[currentDayOfWeekValue] += 1
            }
            currentDayOfWeekValue = (currentDayOfWeekValue + 1) % 7
        }

        // Find common operating days
        val commonDays = mutableListOf<Int>()
        for (idx in 0 until 7) {
            if (dayTypeOperatingCounter[idx] >= dayTypeNonOperatingCounter[idx]) {
                commonDays.add(idx)
            }
        }

        // Find operating exceptions
        val operatingExceptions = mutableMapOf<ConstructWallTimetableUseCase.WallOperationExceptionType, MutableList<LocalDate>>(
            ConstructWallTimetableUseCase.WallOperationExceptionType.ALSO_OPERATES to mutableListOf(),
            ConstructWallTimetableUseCase.WallOperationExceptionType.DOES_NOT_OPERATE to mutableListOf(),
        )
        currentDayOfWeekValue = operatingPeriod.fromDate.dayOfWeek.value - 1
        val firstDate = operatingPeriod.fromDate.toLocalDate()
        for (i in 0 until operatingPeriod.validDays.size) {
            val currentDayOfWeekIsCommon = commonDays.contains(currentDayOfWeekValue)
            val operatesThisDay = operatingPeriod.validDays[i]
            if (operatesThisDay xor currentDayOfWeekIsCommon) {
                val exceptionType = if (operatesThisDay) ConstructWallTimetableUseCase.WallOperationExceptionType.DOES_NOT_OPERATE
                                    else ConstructWallTimetableUseCase.WallOperationExceptionType.ALSO_OPERATES
                operatingExceptions[exceptionType]!!.add(firstDate.plusDays(i.toLong()))
            }
            currentDayOfWeekValue = (currentDayOfWeekValue + 1) % 7
        }

        return ConstructWallTimetableUseCase.WallOperatingDays(
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
    override fun constructWallTimetable(lineVersionId: Long): ConstructWallTimetableUseCase.WallTimetable? {
        val lineVersion = lineVersionJpaRepository.findWallDtoById(lineVersionId).orElse(null) ?: return null
        val activePeriods = activePeriodJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val journeys = journeyJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val operatingPeriods = operatingPeriodJpaRepository.findAllWallDtoByJourneyIds(
            journeys.map(JourneyWallDto::relationalId)
        )
        val scheduledStops = scheduledStopJpaRepository
            .findAllDtoByJourneyIds(journeys.map(JourneyWallDto::relationalId))
            .groupBy(ScheduledStopDto::journeyId)
            .mapValues { (_, routeStops) -> routeStops
                .sortedBy(ScheduledStopDto::stopOrder)
                .map {
                    ConstructWallTimetableUseCase.WallScheduledStop(
                        arrival = if (it.arrival == it.departure) null else it.arrival,
                        departure = it.departure,
                    )
                }
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
                    ConstructWallTimetableUseCase.WallOperatingPeriod(
                        operatingPeriodPair.first,
                        operatingPeriodPair.second,
                        journeysByOperatingPeriod[wallDto.relationalId]!!
                    )
                }
        }

        val reconstructedLineVersion = ConstructWallTimetableUseCase.WallLineVersion(
            relationalId = lineVersion.relationalId,
            publicCode = lineVersion.publicCode,
            name = lineVersion.name,
            shortName = lineVersion.shortName,
            transportMode = lineVersion.transportMode,
            isDetour = lineVersion.isDetour,
            activePeriods = activePeriods,
        )

        return ConstructWallTimetableUseCase.WallTimetable(
            lineVersion = reconstructedLineVersion,
            operatingPeriods = reconstructedOperatingPeriods,
        )
    }
}
