package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParserDataPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort

class ImportDataTimetableUseCase(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
) {
    fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDataPort,
    ) {
        val resultList = mutableListOf<TimetableParserDataPort.TimetableParseResult>()
        var i = 0
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream)
            resultList.add(result)
            operatingPeriodJpaRepository.saveAll(result.operatingPeriods.filter { it.relationalId == null })
            i++
            if (i >= 100) {
                lineVersionJpaRepository.saveAll(resultList.flatMap { it.lineVersions.filter { it.relationalId == null } })
                resultList.flatMap { it.journeys.filter { it.relationalId == null } }.also { journeys ->
                    journeyJpaRepository.saveAll(journeys)
                    scheduledStopJpaRepository.saveAll(journeys.flatMap { it.schedule })
                }
                resultList.clear()
                i = 0
            }
        }
    }
}