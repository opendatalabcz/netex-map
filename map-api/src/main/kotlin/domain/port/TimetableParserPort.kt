package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.model.TimetableStop
import java.io.InputStream

interface TimetableParserPort {
    data class TimetableParseResult(
        val lineVersions: List<LineVersion>,
        val timetableStops: List<TimetableStop>,
        val journeys: List<Journey>,
    )

    fun parseTimetable(contentStream: InputStream): TimetableParseResult
}