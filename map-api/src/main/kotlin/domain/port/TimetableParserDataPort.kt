package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbLineVersion
import java.io.InputStream

interface TimetableParserDataPort {
    data class TimetableParseResult(
        val lineVersions: List<DbLineVersion>,
        val journeys: List<DbJourney>,
    )

    fun parseTimetable(contentStream: InputStream): TimetableParseResult
}