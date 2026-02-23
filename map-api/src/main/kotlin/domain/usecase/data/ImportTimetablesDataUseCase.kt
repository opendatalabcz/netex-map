package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.domain.port.TimetableParserDataPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase

interface ImportTimetablesDataUseCase {
    fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDataPort,
        calculateNextDayOperationDataUseCase: CalculateNextDayOperationDataUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase,
    )
}