package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperationUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.io.InputStream

open class ImportTimetablesUseCaseTransactionWrapper(
    private val delegate: ImportTimetablesUseCase,
    private val transactionTemplate: TransactionTemplate,
): ImportTimetablesUseCase {
    private class LimitedIterator(
        private val delegate: Iterator<InputStream>,
        private val capacity: Int,
    ): Iterator<InputStream> {
        private var serveCount = 0

        override fun hasNext(): Boolean {
            return serveCount < capacity && delegate.hasNext()
        }

        override fun next(): InputStream {
            ++serveCount
            return delegate.next()
        }
    }

    private class IteratorToInputPortAdapter(
        private val iterator: Iterator<InputStream>,
    ): TimetableSourcePort {
        override fun provideInput(): Sequence<InputStream> {
            return iterator.asSequence()
        }
    }

    private object NoOpRouteCalculator: CalculateJourneyRoutesUseCase {
        override fun calculateRoutes() {}
    }

    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
        calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
        calculateJourneyRoutesUseCase: CalculateJourneyRoutesUseCase
    ) {
        val inputStreamSequence = timetableSource.provideInput().iterator()
        while (inputStreamSequence.hasNext()) { transactionTemplate.executeWithoutResult {
            val iteratorWrapper = LimitedIterator(inputStreamSequence, 30)
            delegate.importTimetables(
                IteratorToInputPortAdapter(iteratorWrapper),
                timetableParser,
                calculateNextDayOperationUseCase,
                NoOpRouteCalculator,
            )
        } }
        calculateJourneyRoutesUseCase.calculateRoutes()
    }
}