package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserDataPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase
import cz.cvut.fit.gaierda1.measuring.Measurer
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class ImportTimetablesData(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val transactionTemplate: TransactionTemplate,
): ImportTimetablesDataUseCase {
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDataPort,
        calculateNextDayOperationDataUseCase: CalculateNextDayOperationDataUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase,
    ) {
        val resultList = mutableListOf<TimetableParserDataPort.TimetableParseResult>()
        val inputStreamSequence = timetableSource.provideInput().iterator()
        while (inputStreamSequence.hasNext()) {
            transactionTemplate.executeWithoutResult {
                for (i in 0 until 30) {
                    if (!inputStreamSequence.hasNext()) break
                    val result = timetableParser.parseTimetable(inputStreamSequence.next())
                    resultList.add(result)
                    val newOperatingPeriods = result.operatingPeriods.filter { it.relationalId == null }
                    Measurer.savedOperatingPeriods += newOperatingPeriods.size
                    Measurer.addToDbSave { operatingPeriodJpaRepository.saveAll(newOperatingPeriods) }
                }
                nextDayCalculation(calculateNextDayOperationDataUseCase, resultList)
                batchSave(resultList)
                resultList.clear()
            }
        }
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

    private fun batchSave(resultList: List<TimetableParserDataPort.TimetableParseResult>) {
        val newLineVersions = resultList.flatMap { it.lineVersions.filter { it.relationalId == null } }
        val journeysOfNewLineVersions = resultList.flatMap { it.journeys.filter { newLineVersions.contains(it.lineVersion) } }
        val scheduleStopsOfNewLineVersions = journeysOfNewLineVersions.flatMap { it.schedule }
        Measurer.savedLineVersions += newLineVersions.size
        Measurer.savedJourneys += journeysOfNewLineVersions.size
        Measurer.savedScheduledStops += scheduleStopsOfNewLineVersions.size
        Measurer.addToDbSave {
            lineVersionJpaRepository.saveAll(newLineVersions)
            journeyJpaRepository.saveAll(journeysOfNewLineVersions)
            scheduledStopJpaRepository.saveAll(scheduleStopsOfNewLineVersions)
        }
    }
}