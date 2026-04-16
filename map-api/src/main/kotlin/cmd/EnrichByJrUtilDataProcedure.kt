package cz.cvut.fit.gaierda1.cmd

import cz.cvut.fit.gaierda1.data.filesystem.JrUtilGtfsZipFileSource
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.usecase.load.AddPositionToStopsByNameUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.EnrichBySpacialDataUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.File
import kotlin.time.measureTime

@Component
@Order(1)
class EnrichByJrUtilDataProcedure(
    private val jrUtilGtfsParserPort: JrUtilGtfsParserPort,
    private val addPositionToStopsByNameUseCase: AddPositionToStopsByNameUseCase,
    private val enrichBySpacialDataUseCase: EnrichBySpacialDataUseCase,
    private val calculateRoutesFromWaypointsUseCase: CalculateRoutesFromWaypointsUseCase,
): CommandLineRunner {
    companion object {
        private const val ARGUMENT_PREFIX = "--jrutil-gtfs"
    }

    private val log = LoggerFactory.getLogger(EnrichByJrUtilDataProcedure::class.java)

    override fun run(vararg args: String) {
        val enrichmentArg = args.firstOrNull { it.startsWith(ARGUMENT_PREFIX) }
        if (enrichmentArg == null) return
        val gtfsPath = enrichmentArg
            .substringAfter("=")
            .ifBlank { null }
        if (gtfsPath == null) {
            log.error("Missing file path after $ARGUMENT_PREFIX argument")
            return
        }
        val gtfsFile = File(gtfsPath)
        if (!gtfsFile.exists()) {
            log.error("Enrichment file does not exist: ${gtfsFile.absolutePath}")
            return
        }
        if (!gtfsFile.isFile) {
            log.error("The path after $ARGUMENT_PREFIX argument must denote a regular file")
            return
        }
        val jrUtilGtfsSource = JrUtilGtfsZipFileSource(gtfsFile.inputStream())
        log.info("Begin position enriching")
        log.info("Enriching from file: ${gtfsFile.absolutePath}")
        val enrichTime = measureTime {
            enrichBySpacialDataUseCase.enrichStopsWithPositions(
                jrUtilGtfsSource,
                jrUtilGtfsParserPort,
                addPositionToStopsByNameUseCase,
                calculateRoutesFromWaypointsUseCase,
            )
        }
        log.info("Done position enriching in $enrichTime")
    }
}
