package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.JrUtilGtfsZipFileSource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableZipFileSource
import cz.cvut.fit.gaierda1.data.jrutil.JrUtilGtfsParser
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.load.AddPositionToStopsByNameUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateLineVersionActivePeriods
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateNextDayOperation
import cz.cvut.fit.gaierda1.domain.usecase.load.EnrichStopsWithPositionsUseCase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.io.File
import java.time.OffsetDateTime
import kotlin.system.exitProcess
import kotlin.time.measureTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)
    if (args.firstOrNull { it.startsWith("--import") } != null) doImport(appContext, args)
    if (args.firstOrNull { it.startsWith("--jrutil-gtfs=") } != null) doPositionEnrichment(appContext, args)
    exitProcess(0)
}

fun doPositionEnrichment(appContext: ConfigurableApplicationContext, args: Array<String>) {
    val gtfsPath = args.firstOrNull { it.startsWith("--jrutil-gtfs=") }
        ?.substringAfter("=")
        ?.ifBlank { null }
        ?: error("Missing --jrutil-gtfs argument")
    val gtfsFile = File(gtfsPath)
    val jrUtilGtfsSource = JrUtilGtfsZipFileSource(gtfsFile.inputStream())
    val jrUtilGtfsParser = appContext.getBean(JrUtilGtfsParser::class.java)
    val addPositionsUseCase = appContext.getBean(AddPositionToStopsByNameUseCase::class.java)
    val enrichUseCase = appContext.getBean(EnrichStopsWithPositionsUseCase::class.java)
    println("${OffsetDateTime.now()}: Begin position enriching")
    val enrichTime = measureTime {
        enrichUseCase.enrichStopsWithPositions(jrUtilGtfsSource, jrUtilGtfsParser, addPositionsUseCase)
    }
    println("${OffsetDateTime.now()}: Done position enriching in $enrichTime")
}

fun doImport(appContext: ConfigurableApplicationContext, args: Array<String>) {
    val importArg = args.firstOrNull { it.startsWith("--import") }
    val importPath = importArg
        ?.substringAfter("=")
        ?.ifBlank { null }
        ?: error("Missing --import argument")
    if (importPath.isBlank()) error("Missing import file/directory path")
    val inputFileCount = args.firstOrNull { it.startsWith("--take=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: Int.MAX_VALUE

    val importFile = File(importPath)
    val timetableSource =
        if (importArg.substringBefore("=") == "--import-zip") TimetableZipFileSource(importFile.inputStream(), inputFileCount)
        else TimetableDirectorySource(importFile, inputFileCount)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperation::class.java)
    val calculateJourneyRoutes = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val calculateLineVersionActivePeriods = appContext.getBean(CalculateLineVersionActivePeriods::class.java)
    val importTimetables = appContext.getBean(ImportTimetables::class.java)

    println("${OffsetDateTime.now()}: Begin importing ${if (inputFileCount == Int.MAX_VALUE) "all" else inputFileCount} timetables")
    val importTime = measureTime {
        importTimetables.importTimetables(timetableSource, timetableParser, calculateNextDayOperation)
//        calculateJourneyRoutes.calculateRoutes()
        calculateLineVersionActivePeriods.calculateActivePeriods()
    }
    println("${OffsetDateTime.now()}: Done importing timetables in $importTime")
}
