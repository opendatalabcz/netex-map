package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPattern
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStop
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.Operator
import cz.cvut.fit.gaierda1.data.orm.model.Stop
import cz.cvut.fit.gaierda1.data.orm.model.TariffStop
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyPatternStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatorJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.TariffStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.WithinRegionTransportBanJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class ImportTimetables(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val tariffStopJpaRepository: TariffStopJpaRepository,
    private val stopJpaRepository: StopJpaRepository,
    private val operatorJpaRepository: OperatorJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val journeyPatternJpaRepository: JourneyPatternJpaRepository,
    private val journeyPatternStopJpaRepository: JourneyPatternStopJpaRepository,
    private val withinRegionTransportBanJpaRepository: WithinRegionTransportBanJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    @Value($$"${import.input-timetable-batch-size}")
    private val inputBatchSize: Int,
    @PersistenceContext
    private val entityManager: EntityManager,
): ImportTimetablesUseCase {
    private val log = LoggerFactory.getLogger(ImportTimetables::class.java)

    override fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
        calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
    ) {
        var lineVersionCount = 0
        val inputStreamSequence = timetableSource.provideInput().iterator()
        while (inputStreamSequence.hasNext()) {
            transactionTemplate.executeWithoutResult {
                var cumulativeParseResult = TimetableParseResult()
                for (i in 0 until inputBatchSize) {
                    if (!inputStreamSequence.hasNext()) break
                    lineVersionCount++
                    cumulativeParseResult = timetableParser.parseTimetable(
                        contentStream = inputStreamSequence.next(),
                        cumulativeParseResult = cumulativeParseResult,
                    )
                }
                nextDayCalculation(calculateNextDayOperationUseCase, cumulativeParseResult.journeys)
                batchSave(cumulativeParseResult)
            }
        }
        log.info("Imported $lineVersionCount line versions.")
    }
    
    private fun nextDayCalculation(
        calculateNextDayOperation: CalculateNextDayOperationUseCase,
        journeys: List<Journey>,
    ) {
        for (journey in journeys) {
            calculateNextDayOperation.calculateFor(journey)
        }
    }

    private fun batchSave(
        parseResult: TimetableParseResult,
    ) {
        val newLineVersions = parseResult.lineVersions.filter { it.relationalId == null }
        val newOperators = newLineVersions
            .map { it.operator }
            .filter { it.relationalId == null }
        val tariffStopsOfNewLineVersions = newLineVersions.flatMap { it.tariffStops }
        val stopsOfNewLineVersions = tariffStopsOfNewLineVersions
            .map { it.stopPlace }
            .filter { it.relationalId == null }
        val journeyPatternsOfNewLineVersions = parseResult.journeyPatterns.filter { it.patternId.lineVersionId == null }
        val journeyPatternStopsOfNewLineVersions = journeyPatternsOfNewLineVersions.flatMap { it.patternStops }
        val transportBansOfNewLineVersions = journeyPatternsOfNewLineVersions.flatMap { it.transportBans }
        val journeysOfNewLineVersions = parseResult.journeys.filter { it.journeyPattern.lineVersion.relationalId == null }
        val scheduleStopsOfNewLineVersions = journeysOfNewLineVersions.flatMap { it.schedule }
        val newOperatingPeriodsOfNewLineVersions = journeysOfNewLineVersions
            .map { it.operatingPeriod }
            .filter { it.relationalId == null }

        operatorJpaRepository.saveAll(newOperators)
        operatingPeriodJpaRepository.saveAll(newOperatingPeriodsOfNewLineVersions)
        stopJpaRepository.saveAll(stopsOfNewLineVersions)
        newLineVersions.forEach {
            it.operator =  entityManager.getReference(Operator::class.java, it.operator.relationalId)
        }
        lineVersionJpaRepository.saveAll(newLineVersions)
        tariffStopsOfNewLineVersions.forEach {
            it.stopPlace = entityManager.getReference(Stop::class.java, it.stopPlace.relationalId)
            it.lineVersion = entityManager.getReference(LineVersion::class.java, it.lineVersion.relationalId)
        }
        tariffStopJpaRepository.saveAll(tariffStopsOfNewLineVersions)
        journeyPatternsOfNewLineVersions.forEach {
            it.lineVersion = entityManager.getReference(LineVersion::class.java, it.lineVersion.relationalId)
        }
        journeyPatternJpaRepository.saveAll(journeyPatternsOfNewLineVersions)
        journeyPatternStopsOfNewLineVersions.forEach {
            it.journeyPattern = entityManager.getReference(JourneyPattern::class.java, it.journeyPattern.patternId)
            it.tariffStop = entityManager.getReference(TariffStop::class.java, it.tariffStop.stopId)
        }
        journeyPatternStopJpaRepository.saveAll(journeyPatternStopsOfNewLineVersions)
        transportBansOfNewLineVersions.forEach {
            it.journeyPattern = entityManager.getReference(JourneyPattern::class.java, it.journeyPattern.patternId)
            it.journeyPatternStop = entityManager.getReference(JourneyPatternStop::class.java, it.journeyPatternStop.stopId)
        }
        withinRegionTransportBanJpaRepository.saveAll(transportBansOfNewLineVersions)
        journeysOfNewLineVersions.forEach {
            it.operatingPeriod = entityManager.getReference(OperatingPeriod::class.java, it.operatingPeriod.relationalId)
            it.journeyPattern = entityManager.getReference(JourneyPattern::class.java, it.journeyPattern.patternId)
        }
        journeyJpaRepository.saveAll(journeysOfNewLineVersions)
        scheduleStopsOfNewLineVersions.forEach {
            it.journey = entityManager.getReference(Journey::class.java, it.journey.relationalId)
        }
        scheduledStopJpaRepository.saveAll(scheduleStopsOfNewLineVersions)
    }
}
