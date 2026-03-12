package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserDataPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase
import cz.cvut.fit.gaierda1.measuring.Measurer
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class ImportTimetablesData(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val entityManager: EntityManager,
): ImportTimetablesDataUseCase {
    @Transactional
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDataPort,
        calculateNextDayOperationDataUseCase: CalculateNextDayOperationDataUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase,
    ) {
        val resultList = mutableListOf<TimetableParserDataPort.TimetableParseResult>()
        val operatingPeriodBatchCache = mutableListOf<DbOperatingPeriod>()
        var i = 0
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream, operatingPeriodBatchCache)
            resultList.add(result)
            if (++i >= 100) {
                nextDayCalculation(calculateNextDayOperationDataUseCase, resultList)
                batchSave(resultList, operatingPeriodBatchCache)
                resultList.clear()
                i = 0
            }
        }
        nextDayCalculation(calculateNextDayOperationDataUseCase, resultList)
        batchSave(resultList, operatingPeriodBatchCache)
        calculateJourneyRoutesUseCase.calculateRoutes()
    }
    
    private fun nextDayCalculation(
        calculateNextDayOperation: CalculateNextDayOperationDataUseCase,
        resultList: List<TimetableParserDataPort.TimetableParseResult>,
    ) {
        for (result in resultList) {
            for (journey in result.journeys) {
                calculateNextDayOperation.calculateFor(journey)
            }
        }
    }

    private data class LineVersionDomainKey(
        val externalId: String,
        val isDetour: Boolean,
        val validFrom: LocalDateTime,
        val validTo: LocalDateTime,
        val timezone: ZoneId,
    )

    private fun batchSave(
        resultList: List<TimetableParserDataPort.TimetableParseResult>,
        operatingPeriodBatchCache: List<DbOperatingPeriod>,
    ) {
        val newLineVersions = resultList
            .flatMap { it.lineVersions.filter { it.relationalId == null } }
            .distinctBy { LineVersionDomainKey(it.externalId, it.isDetour, it.validFrom, it.validTo, it.timezone) }
        val journeysOfNewLineVersions = resultList.flatMap { it.journeys.filter { newLineVersions.contains(it.lineVersion) } }
        val scheduleStopsOfNewLineVersions = journeysOfNewLineVersions.flatMap { it.schedule }
        Measurer.savedLineVersions += newLineVersions.size
        Measurer.savedJourneys += journeysOfNewLineVersions.size
        Measurer.savedScheduledStops += scheduleStopsOfNewLineVersions.size
        Measurer.savedOperatingPeriods += operatingPeriodBatchCache.size
        Measurer.addToDbSave {
            operatingPeriodJpaRepository.saveAll(operatingPeriodBatchCache)
            lineVersionJpaRepository.saveAll(newLineVersions)
            journeyJpaRepository.saveAll(journeysOfNewLineVersions)
            scheduledStopJpaRepository.saveAll(scheduleStopsOfNewLineVersions)
            entityManager.flush()
            entityManager.clear()
        }
    }
}