package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.CalculateLineVersionActivePeriods
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperation
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
    if (args.contains("--import")) doImport(appContext, args)
}

fun doImport(appContext: ConfigurableApplicationContext, args: Array<String>) {
    val inputFileCount = args.firstOrNull { it.startsWith("--take=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: Int.MAX_VALUE

    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder, inputFileCount)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperation::class.java)
    val calculateJourneyRoutes = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val calculateLineVersionActivePeriods = appContext.getBean(CalculateLineVersionActivePeriods::class.java)
    val importTimetables = appContext.getBean(ImportTimetables::class.java)

    println("${OffsetDateTime.now()}: Begin importing $inputFileCount timetables")
    val importTime = measureTime {
        importTimetables.importTimetables(
            timetableSource,
            timetableParser,
            calculateNextDayOperation,
            calculateLineVersionActivePeriods,
            calculateJourneyRoutes
        )
    }
    println("${OffsetDateTime.now()}: Done importing timetables in $importTime")
}
