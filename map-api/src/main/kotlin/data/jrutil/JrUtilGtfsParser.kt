package cz.cvut.fit.gaierda1.data.jrutil

import cz.cvut.fit.gaierda1.data.jrutil.model.GtfsJourney
import cz.cvut.fit.gaierda1.data.jrutil.model.GtfsLine
import cz.cvut.fit.gaierda1.data.jrutil.model.GtfsScheduledStop
import cz.cvut.fit.gaierda1.data.jrutil.model.GtfsStop
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.*
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsSourcePort.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

@Component
class JrUtilGtfsParser: JrUtilGtfsParserPort {
    private val csvReader = CSVFormat.Builder.create()
        .setDelimiter(',')
        .setRecordSeparator('\n')
        .setQuote('"')
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreEmptyLines(true)
        .get()

    private fun <T> parseGeneric(inputStream: InputStream, transform: (CSVRecord) -> T): List<T> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val result = mutableListOf<T>()
        reader.use { reader ->
            csvReader.parse(reader).forEach { record ->
                result.add(transform(record))
            }
        }
        return result
    }

    private fun parseLines(linesInputStream: InputStream): List<GtfsLine> {
        return parseGeneric(linesInputStream) { record -> GtfsLine(record["route_id"], record["route_short_name"]) }
    }

    private fun parseJourneys(journeysInputStream: InputStream): List<GtfsJourney> {
        return parseGeneric(journeysInputStream) { record -> GtfsJourney(record["trip_id"], record["route_id"]) }
    }

    private fun parseScheduledStops(scheduledStopsInputStream: InputStream): List<GtfsScheduledStop> {
        return parseGeneric(scheduledStopsInputStream) { record -> GtfsScheduledStop(record["trip_id"], record["stop_id"]) }
    }

    private fun parseStops(stopsInputStream: InputStream): List<GtfsStop> {
        return parseGeneric(stopsInputStream) { record ->
            GtfsStop(record["stop_id"], record["stop_name"], record["stop_lon"].toDouble(), record["stop_lat"].toDouble())
        }
    }

    override fun parseGtfs(gtfsInputStreams: Iterator<GtfsInputStream>): JrUtilGtfsParseResult {
        var stops: List<GtfsStop>? = null
        var linesByKey: Map<String, GtfsLine>? = null
        var scheduledStopByJourneyKey: Map<String, List<GtfsScheduledStop>>? = null
        var journeys: List<GtfsJourney>? = null

        for (gtfsInputStream in gtfsInputStreams) {
            when (gtfsInputStream.type) {
                GtfsFileType.LINES -> {
                    linesByKey = parseLines(gtfsInputStream.inputStream)
                        .associateBy(GtfsLine::lineKey)
                }
                GtfsFileType.JOURNEYS -> {
                    journeys = parseJourneys(gtfsInputStream.inputStream)
                }
                GtfsFileType.SCHEDULED_STOPS -> {
                    scheduledStopByJourneyKey = parseScheduledStops(gtfsInputStream.inputStream)
                        .groupBy(GtfsScheduledStop::journeyKey)
                }
                GtfsFileType.STOPS -> {
                    stops = parseStops(gtfsInputStream.inputStream)
                }
            }
        }
        if (journeys == null || stops == null || scheduledStopByJourneyKey == null || linesByKey == null) {
            error("Missing one or more required GTFS files")
        }

        val resultStopsByPublicCode = mutableMapOf<String, MutableList<GtfsStop>>()
        val stopsByKey = stops.associateBy(GtfsStop::stopKey)
        for (journey in journeys) {
            val scheduledStops = scheduledStopByJourneyKey[journey.journeyKey]!!
            val publicCode = linesByKey[journey.lineKey]!!.publicCode
            val resultStops = resultStopsByPublicCode.getOrPut(publicCode) { mutableListOf() }
            for (scheduledStop in scheduledStops) {
                resultStops.add(stopsByKey[scheduledStop.stopKey]!!)
            }
        }
        val resultLines = resultStopsByPublicCode.map { (publicCode, stops) ->
            JrUtilGtfsLineParseResult(
                publicCode = publicCode,
                stops = stops
                    .distinctBy(GtfsStop::stopKey)
                    .map {
                        JrUtilGtfsStopParseResult(
                            name = it.name,
                            coordinate = Coordinate(it.longitude, it.latitude),
                        )
                    },
            )
        }
        val resultStops = stops.map { JrUtilGtfsStopParseResult(it.name, Coordinate(it.longitude, it.latitude)) }
        return JrUtilGtfsParseResult(resultLines, resultStops)
    }
}
