package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.port.TimetableParserDomainPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.repository.OperatingPeriodRepository

class ImportDomainTimetablesUseCase(
    private val lineVersionRepository: LineVersionRepository,
    private val operatingPeriodRepository: OperatingPeriodRepository,
    private val journeyRepository: JourneyRepository,
) {
    fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDomainPort,
    ) {
        val resultList = mutableListOf<TimetableParserDomainPort.TimetableParseResult>()
        var i = 0
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream)
            resultList.add(result)
            if (++i >= 100) {
                batchSave(resultList)
                resultList.clear()
                i = 0
            }
        }
        batchSave(resultList)
    }

    private fun batchSave(resultList: List<TimetableParserDomainPort.TimetableParseResult>) {
        lineVersionRepository.saveAllIfAbsent(resultList.flatMap { it.lineVersions })
        operatingPeriodRepository.saveAllIfAbsent(resultList.flatMap { it.operatingPeriods })
        journeyRepository.saveAllIfAbsent(resultList.flatMap { it.journeys })
    }
}