package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperation
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
    val inputFileCount = args.firstOrNull { it.startsWith("--take=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: Int.MAX_VALUE
    doImport(appContext, inputFileCount)
    exitProcess(0)
}

fun doImport(appContext: ConfigurableApplicationContext, inputFileCount: Int) {
    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder, inputFileCount)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperationData = appContext.getBean(CalculateNextDayOperation::class.java)
    val calculateJourneyRoutesData = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val importTimetablesData = appContext.getBean(ImportTimetables::class.java)

    println("${OffsetDateTime.now()}: Begin importing $inputFileCount timetables")
    val importTime = measureTime {
        importTimetablesData.importTimetables(timetableSource, timetableParser, calculateNextDayOperationData, calculateJourneyRoutesData)
    }
    println("${OffsetDateTime.now()}: Done importing timetables in $importTime")
}
