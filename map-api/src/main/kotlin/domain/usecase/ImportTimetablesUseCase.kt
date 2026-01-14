package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.repository.TimetableStopRepository

class ImportTimetablesUseCase(
    private val lineRepository: LineRepository,
    private val lineVersionRepository: LineVersionRepository,
    private val timetableStopRepository: TimetableStopRepository,
    private val journeyRepository: JourneyRepository,
) {
    fun importTimetables(
        timetableSource: TimetableSourcePort,
        timetableParser: TimetableParserPort,
    ) {
        timetableSource.provideInput { entryContentStream ->
            val result = timetableParser.parseTimetable(entryContentStream)
            result.lines.forEach(lineRepository::save)
            result.lineVersions.forEach(lineVersionRepository::save)
            result.timetableStops.forEach(timetableStopRepository::save)
            result.journeys.forEach(journeyRepository::save)
        }
    }
}