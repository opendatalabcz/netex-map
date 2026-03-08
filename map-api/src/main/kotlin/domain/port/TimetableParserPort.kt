package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import java.io.InputStream

interface TimetableParserPort {
    data class TimetableParseResult(
        val lineVersions: List<LineVersion>,
        val operatingPeriods: List<OperatingPeriod>,
        val journeys: List<Journey>,
    )

    fun parseTimetable(contentStream: InputStream): TimetableParseResult
}