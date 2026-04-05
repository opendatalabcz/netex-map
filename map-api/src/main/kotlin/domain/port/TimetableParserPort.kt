package cz.cvut.fit.gaierda1.domain.port

import java.io.InputStream

interface TimetableParserPort {
    fun parseTimetable(
        contentStream: InputStream,
        cumulativeParseResult: TimetableParseResult? = null,
    ): TimetableParseResult
}
