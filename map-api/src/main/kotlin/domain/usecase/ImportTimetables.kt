package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime

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
        val operatingPeriodBatchCache = mutableListOf<OperatingPeriod>()
        val inputStreamSequence = timetableSource.provideInput().iterator()
        while (inputStreamSequence.hasNext()) {
            transactionTemplate.executeWithoutResult {
                for (i in 0 until 30) {
                    if (!inputStreamSequence.hasNext()) break
                    val result = timetableParser.parseTimetable(inputStreamSequence.next(), operatingPeriodBatchCache)
                    resultList.add(result)
                }
                nextDayCalculation(calculateNextDayOperationUseCase, resultList)
                batchSave(resultList, operatingPeriodBatchCache)
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

    private data class LineVersionDomainKey(
        val publicCode: String,
        val isDetour: Boolean,
        val validFrom: OffsetDateTime,
        val validTo: OffsetDateTime,
    )

    private fun batchSave(
        resultList: List<TimetableParserPort.TimetableParseResult>,
        operatingPeriodBatchCache: List<OperatingPeriod>,
    ) {
        val newLineVersions = resultList
            .flatMap { it.lineVersions.filter { it.relationalId == null } }
            .distinctBy { LineVersionDomainKey(it.publicCode, it.isDetour, it.validFrom, it.validTo) }
        val journeysOfNewLineVersions = resultList.flatMap { it.journeys.filter { newLineVersions.contains(it.lineVersion) } }
        val scheduleStopsOfNewLineVersions = journeysOfNewLineVersions.flatMap { it.schedule }
        operatingPeriodJpaRepository.saveAll(operatingPeriodBatchCache)
        lineVersionJpaRepository.saveAll(newLineVersions)
        journeyJpaRepository.saveAll(journeysOfNewLineVersions)
        scheduledStopJpaRepository.saveAll(scheduleStopsOfNewLineVersions)
    }
}