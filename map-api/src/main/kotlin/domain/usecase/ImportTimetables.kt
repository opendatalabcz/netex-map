package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository

class ImportTimetables(
    private val journeyRepository: JourneyRepository,
): ImportTimetablesUseCase {
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
        calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase,
    ) {
        val resultList = mutableListOf<TimetableParserPort.TimetableParseResult>()
        val inputStreamSequence = timetableSource.provideInput().iterator()
        while (inputStreamSequence.hasNext()) {
            for (i in 0 until 30) {
                if (!inputStreamSequence.hasNext()) break
                val result = timetableParser.parseTimetable(inputStreamSequence.next())
                resultList.add(result)
            }
            nextDayCalculation(calculateNextDayOperationUseCase, resultList)
            batchSave(resultList)
            resultList.clear()
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
        journeyRepository.saveAllIfAbsent(resultList.flatMap { it.journeys })
    }
}