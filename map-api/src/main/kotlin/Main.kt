package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.JrUtilGtfsZipFileSource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableZipFileSource
import cz.cvut.fit.gaierda1.data.jrutil.JrUtilGtfsParser
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.load.AddPositionToStopsByNameUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateLineVersionActivePeriods
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateNextDayOperation
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateRoutesFromWaypointsUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.EnrichBySpacialDataUseCase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.io.File
import java.time.OffsetDateTime
import kotlin.time.measureTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)
    if (args.firstOrNull { it.startsWith("--import") } != null) doImport(appContext, args)
    if (args.firstOrNull { it.startsWith("--jrutil-gtfs=") } != null) doPositionEnrichment(appContext, args)
}

fun doPositionEnrichment(appContext: ConfigurableApplicationContext, args: Array<String>) {
    val gtfsPath = args.firstOrNull { it.startsWith("--jrutil-gtfs=") }
        ?.substringAfter("=")
        ?.ifBlank { null }
        ?: error("Missing file path after --jrutil-gtfs= argument")
    val gtfsFile = File(gtfsPath)
    val jrUtilGtfsSource = JrUtilGtfsZipFileSource(gtfsFile.inputStream())
    val jrUtilGtfsParser = appContext.getBean(JrUtilGtfsParser::class.java)
    val addPositionsUseCase = appContext.getBean(AddPositionToStopsByNameUseCase::class.java)
    val enrichUseCase = appContext.getBean(EnrichBySpacialDataUseCase::class.java)
    val routeCalculator = appContext.getBean(CalculateRoutesFromWaypointsUseCase::class.java)
    println("${OffsetDateTime.now()}: Begin position enriching")
    println("Enriching from file: ${gtfsFile.absolutePath}")
    val enrichTime = measureTime {
        enrichUseCase.enrichStopsWithPositions(jrUtilGtfsSource, jrUtilGtfsParser, addPositionsUseCase, routeCalculator)
    }
    println("${OffsetDateTime.now()}: Done position enriching in $enrichTime")
}

fun doImport(appContext: ConfigurableApplicationContext, args: Array<String>) {
    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperation::class.java)
    val calculateLineVersionActivePeriods = appContext.getBean(CalculateLineVersionActivePeriods::class.java)
    val importTimetables = appContext.getBean(ImportTimetables::class.java)

    println("${OffsetDateTime.now()}: Begin importing timetables")
    val importTime = measureTime {
        for (importArg in args.filter { it.startsWith("--import") && it.contains("=") }) {
            val importPath = importArg
                .substringAfter("=")
                .ifBlank { null }
            if (importPath == null) {
                System.err.println("Missing import file/directory path after --import argument")
                continue
            }
            val importFile = File(importPath)
            println("Importing from file/directory: ${importFile.absolutePath}")
            val timetableSource =
                if (importArg.substringBefore("=") == "--import-zip") TimetableZipFileSource(importFile.inputStream())
                else TimetableDirectorySource(importFile)
            importTimetables.importTimetables(timetableSource, timetableParser, calculateNextDayOperation)
        }
        calculateLineVersionActivePeriods.calculateActivePeriods()
    }
    println("${OffsetDateTime.now()}: Done importing timetables in $importTime")
}
