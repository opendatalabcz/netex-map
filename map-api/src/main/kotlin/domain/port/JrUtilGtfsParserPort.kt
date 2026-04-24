package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop

interface JrUtilGtfsParserPort {
    data class JrUtilGtfsLineParseResult(
        val publicCode: String,
        val stops: List<PhysicalStop>,
    )
    data class JrUtilGtfsParseResult(
        val lines: List<JrUtilGtfsLineParseResult>,
        val allStops: List<PhysicalStop>,
    )
    fun parseGtfs(gtfsInputStreams: Iterator<JrUtilGtfsSourcePort.GtfsInputStream>): JrUtilGtfsParseResult
}
