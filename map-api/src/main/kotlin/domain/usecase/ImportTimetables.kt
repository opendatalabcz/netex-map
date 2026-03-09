package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class ImportTimetables(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val transactionTemplate: TransactionTemplate,
): ImportTimetablesUseCase {
    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
        calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
    ) {
        val resultList = mutableListOf<TimetableParserPort.TimetableParseResult>()
        val inputStreamSequence = timetableSource.provideInput().iterator()
        while (inputStreamSequence.hasNext()) {
            transactionTemplate.executeWithoutResult {
                for (i in 0 until 30) {
                    if (!inputStreamSequence.hasNext()) break
                    val result = timetableParser.parseTimetable(inputStreamSequence.next())
                    resultList.add(result)
                    val newOperatingPeriods = result.operatingPeriods.filter { it.relationalId == null }
                    operatingPeriodJpaRepository.saveAll(newOperatingPeriods)
                }
                nextDayCalculation(calculateNextDayOperationUseCase, resultList)
                batchSave(resultList)
                resultList.clear()
            }
        }
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
        val newLineVersions = resultList.flatMap { it.lineVersions.filter { it.relationalId == null } }
        val journeysOfNewLineVersions = resultList.flatMap { it.journeys.filter { newLineVersions.contains(it.lineVersion) } }
        val scheduleStopsOfNewLineVersions = journeysOfNewLineVersions.flatMap { it.schedule }
        lineVersionJpaRepository.saveAll(newLineVersions)
        journeyJpaRepository.saveAll(journeysOfNewLineVersions)
        scheduledStopJpaRepository.saveAll(scheduleStopsOfNewLineVersions)
    }
}