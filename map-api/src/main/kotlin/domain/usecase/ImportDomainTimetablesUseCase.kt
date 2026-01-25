package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.port.TimetableParserDomainPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository

class ImportDomainTimetablesUseCase(
    private val lineVersionRepository: LineVersionRepository,
    private val journeyRepository: JourneyRepository,
) {
    fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserDomainPort,
    ) {
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream)
            result.lineVersions.forEach(lineVersionRepository::save)
            result.journeys.forEach(journeyRepository::save)
        }
    }
}