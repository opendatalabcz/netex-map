package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperationUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import org.springframework.transaction.annotation.Transactional

open class ImportTimetablesUseCaseTransactionWrapper(
    private val delegate: ImportTimetablesUseCase,
): ImportTimetablesUseCase {
    @Transactional
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
        calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase
    ) {
        return delegate.importTimetables(timetableSource, timetableParser, calculateNextDayOperationUseCase, calculateJourneyRoutesUseCase)
    }
}