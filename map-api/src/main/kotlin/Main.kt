package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.data.TimetableDataParser
import cz.cvut.fit.gaierda1.data.netex.domain.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperationUseCase
import cz.cvut.fit.gaierda1.domain.usecase.data.ImportTimetablesData
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.data.CalculateJourneyRoutesDataMock
import cz.cvut.fit.gaierda1.domain.usecase.data.CalculateNextDayOperationData
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
    val useDomainImport = !args.contains("--data")
    val inputFileCount = args.firstOrNull { it.startsWith("--take=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: Int.MAX_VALUE
    doImport(appContext, useDomainImport, inputFileCount)
    exitProcess(0)
}

fun doImport(appContext: ConfigurableApplicationContext, useDomainImport: Boolean, inputFileCount: Int) {
    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder, inputFileCount)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperationUseCase::class.java)
    val calculateJourneyRoutes = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val importTimetable = appContext.getBean(ImportTimetablesUseCase::class.java)

    val timetableDataParser = appContext.getBean(TimetableDataParser::class.java)
    val calculateNextDayOperationData = appContext.getBean(CalculateNextDayOperationData::class.java)
    val calculateJourneyRoutesData = appContext.getBean(CalculateJourneyRoutesDataMock::class.java)
    val importTimetablesData = appContext.getBean(ImportTimetablesData::class.java)

    println("${OffsetDateTime.now()}: Begin importing $inputFileCount timetables using ${if (useDomainImport) "domain" else "data"} model")
    val importTime = measureTime { if (useDomainImport) {
        importTimetable.importTimetables(timetableSource, timetableParser, calculateNextDayOperation, calculateJourneyRoutes)
    } else {
        importTimetablesData.importTimetables(timetableSource, timetableDataParser, calculateNextDayOperationData, calculateJourneyRoutesData)
    } }
    println("${OffsetDateTime.now()}: Done importing timetables in $importTime")
}
