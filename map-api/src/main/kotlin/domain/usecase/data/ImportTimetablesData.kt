package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserDataPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase

class ImportTimetablesData(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
): ImportTimetablesDataUseCase {
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDataPort,
        calculateNextDayOperationDataUseCase: CalculateNextDayOperationDataUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase,
    ) {
        val resultList = mutableListOf<TimetableParserDataPort.TimetableParseResult>()
        var i = 0
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream)
            resultList.add(result)
            operatingPeriodJpaRepository.saveAll(result.operatingPeriods.filter { it.relationalId == null })
            if (++i >= 100) {
                nextDayCalculation(calculateNextDayOperationDataUseCase, resultList)
                batchSave(resultList)
                resultList.clear()
                i = 0
            }
        }
        nextDayCalculation(calculateNextDayOperationDataUseCase, resultList)
        batchSave(resultList)
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
        lineVersionJpaRepository.saveAll(newLineVersions)
        journeyJpaRepository.saveAll(journeysOfNewLineVersions)
        scheduledStopJpaRepository.saveAll(journeysOfNewLineVersions.flatMap { it.schedule })
    }
}