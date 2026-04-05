package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.model.JourneyDirectionType
import cz.cvut.fit.gaierda1.data.orm.model.LineType
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
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatingPeriodDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyPatternStopWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.TariffStopWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.WithinRegionTransportBanWallDto
import cz.cvut.fit.gaierda1.domain.usecase.view.ConstructWallTimetableUseCase.*
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
    private val operatorJpaRepository: OperatorJpaRepository,
    private val tariffStopJpaRepository: TariffStopJpaRepository,
    private val stopJpaRepository: StopJpaRepository,
    private val journeyPatternJpaRepository: JourneyPatternJpaRepository,
    private val journeyPatternStopJpaRepository: JourneyPatternStopJpaRepository,
    private val withinRegionTransportBanJpaRepository: WithinRegionTransportBanJpaRepository,
): ConstructWallTimetableUseCase {
    private fun reconstructWallOperatingPeriod(
        operatingPeriod: OperatingPeriodDto,
    ): Pair<WallOperatingDays, Map<WallOperationExceptionType, List<LocalDate>>> {
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

    private fun reconstructLineVersion(lineVersionId: Long): WallLineVersion? {
        val lineVersion = lineVersionJpaRepository.findDtoById(lineVersionId).orElse(null) ?: return null
        val operator = operatorJpaRepository.findDtoByOperatorId(lineVersion.operatorId).orElse(null) ?: return null
        val activePeriods = activePeriodJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val tariffStops = tariffStopJpaRepository
            .findAllWallDtoByLineVersionId(lineVersionId)
            .sortedBy(TariffStopWallDto::tariffOrder)
            .map { WallTariffStop(
                tariffZone = it.tariffZone,
                stopId = it.stopId,
            ) }
        val stops = stopJpaRepository.findAllDtoByStopIds(
            tariffStops.map(WallTariffStop::stopId)
        )

        return WallLineVersion(
            relationalId = lineVersion.relationalId,
            publicCode = lineVersion.publicCode,
            name = lineVersion.name,
            shortName = lineVersion.shortName,
            transportMode = lineVersion.transportMode,
            isDetour = lineVersion.isDetour,
            activePeriods = activePeriods,
            lineType = LineType.fromJdfCode(lineVersion.lineType),
            operator = operator,
            tariffStops = tariffStops,
            stops = stops,
        )
    }

    private fun reconstructOperatingPeriods(lineVersionId: Long): List<WallOperatingPeriod> {
        val journeys = journeyJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val operatingPeriods = operatingPeriodJpaRepository.findAllWallDtoByJourneyIds(
            journeys.map(JourneyWallDto::relationalId)
        )
        val scheduledStops = scheduledStopJpaRepository
            .findAllDtoByJourneyIds(journeys.map(JourneyWallDto::relationalId))
            .groupBy(ScheduledStopDto::journeyId)
            .mapValues { (_, routeStops) -> routeStops
                .sortedBy(ScheduledStopDto::stopOrder)
                .map { WallScheduledStop(
                    arrival = if (it.arrival == it.departure) null else it.arrival,
                    departure = it.departure,
                ) }
            }

        val journeysByOperatingPeriod = journeys
            .groupBy(JourneyWallDto::operatingPeriodId)
            .mapValues { (_, journeys) ->
                journeys.map { journey -> WallJourney(
                    relationalId = journey.relationalId,
                    schedule = scheduledStops[journey.relationalId]!!,
                    requiresOrdering = journey.requiresOrdering,
                    baggageStorage = journey.baggageStorage,
                    cyclesAllowed = journey.cyclesAllowed,
                    lowFloorAccess = journey.lowFloorAccess,
                    reservationCompulsory = journey.reservationCompulsory,
                    reservationPossible = journey.reservationPossible,
                    snacksOnBoard = journey.snacksOnBoard,
                    unaccompaniedMinorAssistance = journey.unaccompaniedMinorAssistance,
                ) }
            }

        return operatingPeriods.map { wallDto ->
            reconstructWallOperatingPeriod(wallDto)
                .let { operatingPeriodPair ->
                    WallOperatingPeriod(
                        operatingPeriodPair.first,
                        operatingPeriodPair.second,
                        journeysByOperatingPeriod[wallDto.relationalId]!!,
                    )
                }
        }
    }

    private fun reconstructJourneyPatterns(lineVersionId: Long): List<WallJourneyPattern> {
        val journeyPatterns = journeyPatternJpaRepository.findAllWallDtoByLineVersionId(lineVersionId)
        val journeyPatternStops = journeyPatternStopJpaRepository
            .findAllWallDtoByLineVersionId(lineVersionId)
            .groupBy(JourneyPatternStopWallDto::patternNumber)
            .mapValues { (_, stops) ->
                stops.sortedBy(JourneyPatternStopWallDto::stopOrder)
                    .map { WallJourneyPatternStop(
                        tariffOrder = it.tariffOrder,
                        distanceToNextStop = it.distanceToNextStop,
                        forBoarding = it.forBoarding,
                        forAlighting = it.forAlighting,
                        requiresOrdering = it.requiresOrdering,
                        stopOnRequest = it.stopOnRequest,
                    ) }
            }
        val transportBans = withinRegionTransportBanJpaRepository
            .findAllWallDtoByLineVersionId(lineVersionId)
            .groupBy(WithinRegionTransportBanWallDto::patternNumber)
            .mapValues { (_, bans) ->
                bans.groupBy { it.banGroupNumber }
                    .mapValues { (_, bans) ->
                        bans.map(WithinRegionTransportBanWallDto::stopOrder)
                    }
            }

        return journeyPatterns.map { journeyPattern -> WallJourneyPattern(
            patternNumber = journeyPattern.patternNumber,
            direction = JourneyDirectionType.fromShortCode(journeyPattern.direction),
            stops = journeyPatternStops[journeyPattern.patternNumber]!!,
            transportBans = transportBans[journeyPattern.patternNumber]?.values?.toList(),
            routeId = journeyPattern.routeId,
        ) }
    }

    @Transactional(readOnly = true)
    override fun constructWallTimetable(lineVersionId: Long): WallTimetable? {
        return WallTimetable(
            lineVersion = reconstructLineVersion(lineVersionId) ?: return null,
            operatingPeriods = reconstructOperatingPeriods(lineVersionId),
            journeyPatterns = reconstructJourneyPatterns(lineVersionId),
        )
    }
}
