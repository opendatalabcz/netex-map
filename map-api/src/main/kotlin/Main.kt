package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableZipFileSource
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.load.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateLineVersionActivePeriods
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateNextDayOperation
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
    if (args.firstOrNull { it.startsWith("--import=") } != null) doImport(appContext, args)
}

fun doImport(appContext: ConfigurableApplicationContext, args: Array<String>) {
    val importPath = args.firstOrNull { it.startsWith("--import=") }?.substringAfter("=") ?: error("Missing --import argument")
    if (importPath.isBlank()) error("Missing import file/directory path")
    val inputFileCount = args.firstOrNull { it.startsWith("--take=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: Int.MAX_VALUE

    val importFile = File(importPath)
    val timetableSource =
        if (args.contains("--zip")) TimetableZipFileSource(importFile.inputStream(), inputFileCount)
        else TimetableDirectorySource(importFile, inputFileCount)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperation::class.java)
    val calculateJourneyRoutes = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val calculateLineVersionActivePeriods = appContext.getBean(CalculateLineVersionActivePeriods::class.java)
    val importTimetables = appContext.getBean(ImportTimetables::class.java)

    println("${OffsetDateTime.now()}: Begin importing ${if (inputFileCount == Int.MAX_VALUE) "all" else inputFileCount} timetables")
    val importTime = measureTime {
        importTimetables.importTimetables(timetableSource, timetableParser, calculateNextDayOperation)
        calculateJourneyRoutes.calculateRoutes()
        calculateLineVersionActivePeriods.calculateActivePeriods()
    }
    println("${OffsetDateTime.now()}: Done importing timetables in $importTime")
}
