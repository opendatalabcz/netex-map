package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.repository.OperatingPeriodRepository

class ImportTimetables(
    private val lineVersionRepository: LineVersionRepository,
    private val operatingPeriodRepository: OperatingPeriodRepository,
    private val journeyRepository: JourneyRepository,
): ImportTimetablesUseCase {
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
        calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase,
    ) {
        val resultList = mutableListOf<TimetableParserPort.TimetableParseResult>()
        var i = 0
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream)
            resultList.add(result)
            if (++i >= 100) {
                nextDayCalculation(calculateNextDayOperationUseCase, resultList)
                batchSave(resultList)
                resultList.clear()
                i = 0
            }
        }
        nextDayCalculation(calculateNextDayOperationUseCase, resultList)
        batchSave(resultList)
        calculateJourneyRoutesUseCase.calculateRoutes()
    }

    private fun nextDayCalculation(
        calculateNextDayOperation: CalculateNextDayOperationUseCase,
        resultList: List<TimetableParserPort.TimetableParseResult>,
    ) {
        for (result in resultList) {
            for (journey in result.journeys) {
                calculateNextDayOperation.calculateFor(journey)
            }
        }
    }

    private fun batchSave(resultList: List<TimetableParserPort.TimetableParseResult>) {
        lineVersionRepository.saveAllIfAbsent(resultList.flatMap { it.lineVersions })
        operatingPeriodRepository.saveAllIfAbsent(resultList.flatMap { it.operatingPeriods })
        journeyRepository.saveAllIfAbsent(resultList.flatMap { it.journeys })
    }
}