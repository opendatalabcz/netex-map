package cz.cvut.fit.gaierda1.cmd

import cz.cvut.fit.gaierda1.data.filesystem.JrUtilGtfsZipFileSource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableZipFileSource
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.port.OsmParserPort
import cz.cvut.fit.gaierda1.domain.usecase.load.PairJrUtilStopsWithStopsUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateLineVersionActivePeriodsUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateNextDayOperationUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.EnrichBySpacialDataUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.ImportPhysicalStopsFromOsmUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.ImportTimetablesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.NormalizeStopNameUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.RoughlyPairPhysicalStopsWithStopsUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File
import kotlin.text.substringAfter
import kotlin.time.measureTime

@Component
class ImportProcedure(
    private val timetableParser: TimetableParser,
    private val calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
    private val calculateLineVersionActivePeriodsUseCase: CalculateLineVersionActivePeriodsUseCase,
    private val importTimetablesUseCase: ImportTimetablesUseCase,
    private val jrUtilGtfsParserPort: JrUtilGtfsParserPort,
    private val pairJrUtilStopsWithStopsUseCase: PairJrUtilStopsWithStopsUseCase,
    private val enrichBySpacialDataUseCase: EnrichBySpacialDataUseCase,
    private val calculateRoutesFromWaypointsUseCase: CalculateRoutesFromWaypointsUseCase,
    private val osmParserPort: OsmParserPort,
    private val normalizeStopNameUseCase: NormalizeStopNameUseCase,
    private val importPhysicalStopsFromOsmUseCase: ImportPhysicalStopsFromOsmUseCase,
    private val roughlyPairPhysicalStopsWithStopsUseCase: RoughlyPairPhysicalStopsWithStopsUseCase,
): CommandLineRunner {
    companion object {
        private const val COMMON_NETEX_IMPORT_ARGUMENT_PREFIX = "--netex"
        private const val ZIP_NETEX_IMPORT_ARGUMENT_PREFIX = "$COMMON_NETEX_IMPORT_ARGUMENT_PREFIX-zip"
        private const val ENRICH_ARGUMENT_PREFIX = "--jrutil-gtfs"
        private const val STOP_IMPORT_ARGUMENT = "--osm-pbf"
    }

    private val log = LoggerFactory.getLogger(ImportProcedure::class.java)

    private fun enrich(enrichmentArg: String) {
        val gtfsPath = enrichmentArg
            .substringAfter("=")
            .ifBlank { null }
        if (gtfsPath == null) {
            log.error("Missing file path after $ENRICH_ARGUMENT_PREFIX argument")
            return
        }
        val gtfsFile = File(gtfsPath)
        if (!gtfsFile.exists()) {
            log.error("Enrichment file does not exist: ${gtfsFile.absolutePath}")
            return
        }
        if (!gtfsFile.isFile) {
            log.error("The path after $ENRICH_ARGUMENT_PREFIX argument must denote a regular file")
            return
        }
        val jrUtilGtfsSource = JrUtilGtfsZipFileSource(gtfsFile.inputStream())
        log.info("Begin position enriching")
        log.info("Enriching from file: ${gtfsFile.absolutePath}")
        val enrichTime = measureTime {
            enrichBySpacialDataUseCase.enrichStopsWithPositions(
                jrUtilGtfsSource,
                jrUtilGtfsParserPort,
                normalizeStopNameUseCase,
                roughlyPairPhysicalStopsWithStopsUseCase,
                pairJrUtilStopsWithStopsUseCase,
                calculateRoutesFromWaypointsUseCase,
            )
        }
        log.info("Done position enriching in $enrichTime")
    }

    private fun netexImport(importArgs: List<String>) {
        log.info("Begin importing timetables")
        val importTime = measureTime {
            for (importArg in importArgs) {
                val importPath = importArg
                    .substringAfter("=")
                    .ifBlank { null }
                if (importPath == null) {
                    log.error("Missing import file/directory path after $COMMON_NETEX_IMPORT_ARGUMENT_PREFIX argument")
                    continue
                }
                val importFile = File(importPath)
                if (!importFile.exists()) {
                    log.error("Import file/directory path does not exist: ${importFile.absolutePath}")
                    continue
                }
                val useZip = importArg.substringBefore("=") == ZIP_NETEX_IMPORT_ARGUMENT_PREFIX
                if (useZip && !importFile.isFile) {
                    log.error("The path after $ZIP_NETEX_IMPORT_ARGUMENT_PREFIX argument must denote a regular file")
                    continue
                } else if (!useZip && !importFile.isDirectory) {
                    log.error("The path after $COMMON_NETEX_IMPORT_ARGUMENT_PREFIX argument must denote a directory")
                    continue
                }
                log.info("Importing from ${if (useZip) "file" else "directory"}: ${importFile.absolutePath}")
                val timetableSource =
                    if (useZip) TimetableZipFileSource(importFile.inputStream())
                    else TimetableDirectorySource(importFile)
                importTimetablesUseCase.importTimetables(
                    timetableSource,
                    timetableParser,
                    calculateNextDayOperationUseCase,
                )
            }
            calculateLineVersionActivePeriodsUseCase.calculateActivePeriods()
        }
        log.info("Done importing timetables in $importTime")

    }

    private fun stopImport(importArg: String) {
        val osmPbfPath = importArg
            .substringAfter("=")
            .ifBlank { null }
        if (osmPbfPath == null) {
            log.error("Missing file path after $STOP_IMPORT_ARGUMENT argument")
            return
        }
        val osmPbfFile = File(osmPbfPath)
        if (!osmPbfFile.exists()) {
            log.error("Import file does not exist: ${osmPbfFile.absolutePath}")
            return
        }
        if (!osmPbfFile.isFile) {
            log.error("The path after $STOP_IMPORT_ARGUMENT argument must denote a regular file")
            return
        }
        log.info("Begin importing physical stops")
        log.info("Importing from file: ${osmPbfFile.absolutePath}")
        val importTime = measureTime {
            importPhysicalStopsFromOsmUseCase.importPhysicalStopsFromOsm(
                osmPbfFile,
                osmParserPort,
                normalizeStopNameUseCase,
            )
        }
        log.info("Done importing physical stops in $importTime")
    }

    override fun run(vararg args: String) {
        val importArgs = args.filter { it.startsWith(COMMON_NETEX_IMPORT_ARGUMENT_PREFIX) }
        if (importArgs.isNotEmpty()) netexImport(importArgs)
        val stopImportArg = args.firstOrNull { it.startsWith(STOP_IMPORT_ARGUMENT) }
        if (stopImportArg != null) stopImport(stopImportArg)
        val enrichmentArg = args.firstOrNull { it.startsWith(ENRICH_ARGUMENT_PREFIX) }
        if (enrichmentArg != null) enrich(enrichmentArg)
    }
}
