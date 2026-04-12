package cz.cvut.fit.gaierda1.domain.port

interface JrUtilGtfsParserPort {
    data class JrUtilGtfsStopParseResult(
        val name: String,
        val longitude: Double,
        val latitude: Double,
    )
    data class JrUtilGtfsLineParseResult(
        val publicCode: String,
        val stops: List<JrUtilGtfsStopParseResult>,
    )
    data class JrUtilGtfsParseResult(
        val lines: List<JrUtilGtfsLineParseResult>,
        val allStops: List<JrUtilGtfsStopParseResult>,
    )
    fun parseGtfs(gtfsInputStreams: Iterator<JrUtilGtfsSourcePort.GtfsInputStream>): JrUtilGtfsParseResult
}
