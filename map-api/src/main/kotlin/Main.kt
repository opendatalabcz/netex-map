package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.data.TimetableDataParser
import cz.cvut.fit.gaierda1.data.netex.domain.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperationUseCase
import cz.cvut.fit.gaierda1.domain.usecase.data.ImportTimetablesData
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.data.CalculateJourneyRoutesDataMock
import cz.cvut.fit.gaierda1.domain.usecase.data.CalculateNextDayOperationData
import cz.cvut.fit.gaierda1.measuring.Measurer
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.time.OffsetDateTime
import kotlin.system.exitProcess
import kotlin.time.measureTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)
    val outputStream = openOutputStreamOrNull(args)
    val useDomainImport = !args.contains("--data")
    val inputFileCount = args.firstOrNull { it.startsWith("--take=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: Int.MAX_VALUE
    outputStream?.use { out ->
        doImport(appContext, useDomainImport, out, inputFileCount)
    } ?: doImport(appContext, useDomainImport, System.out, inputFileCount)
    exitProcess(0)
}

private fun openOutputStreamOrNull(args: Array<String>): OutputStream? {
    val outPath = args.firstOrNull { it.startsWith("--out=") }
        ?.substringAfter("=")
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val file = File(outPath).apply {
        parentFile?.mkdirs()
        if (!exists()) createNewFile()
    }

    return FileOutputStream(file, true)
}

fun doImport(appContext: ConfigurableApplicationContext, useDomainImport: Boolean, outputStream: OutputStream, inputFileCount: Int) {
    val entityManagerFactory = appContext.getBean(EntityManagerFactory::class.java)
    val sessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
    val stats = sessionFactory.statistics

    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder, inputFileCount)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperationUseCase::class.java)
    val calculateJourneyRoutes = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val importTimetable = appContext.getBean(ImportTimetables::class.java)

    val timetableDataParser = appContext.getBean(TimetableDataParser::class.java)
    val calculateNextDayOperationData = appContext.getBean(CalculateNextDayOperationData::class.java)
    val calculateJourneyRoutesData = appContext.getBean(CalculateJourneyRoutesDataMock::class.java)
    val importTimetablesData = appContext.getBean(ImportTimetablesData::class.java)

    val writer = PrintWriter(outputStream)
    writer.println("${OffsetDateTime.now()}: Begin importing $inputFileCount timetables using ${if (useDomainImport) "domain" else "data"} model")
    writer.flush()
    val importTime = measureTime { if (useDomainImport) {
        importTimetable.importTimetables(timetableSource, timetableParser, calculateNextDayOperation, calculateJourneyRoutes)
    } else {
        importTimetablesData.importTimetables(timetableSource, timetableDataParser, calculateNextDayOperationData, calculateJourneyRoutesData)
    } }
    writer.println("${OffsetDateTime.now()}: Done importing timetables in $importTime")

    writer.println("Query count: ${stats.queryExecutionCount}")
    writer.println("App initiated finds: ${Measurer.appInitiatedFinds}")
    writer.println("Insert count: ${stats.entityInsertCount}")
    writer.println("Update count: ${stats.entityUpdateCount}")
    writer.println("Prepared statement count: ${stats.prepareStatementCount}")
    writer.println("Session open count: ${stats.sessionOpenCount}")
    writer.println("Transaction count: ${stats.transactionCount}")
    writer.println("-----------------------------------")
    writer.println("Saved Journeys: ${Measurer.savedJourneys}")
    writer.println("Saved Scheduled Stops: ${Measurer.savedScheduledStops}")
    writer.println("Saved Physical Stops: ${Measurer.savedPhysicalStops}")
    writer.println("Saved Route Stops: ${Measurer.savedRouteStops}")
    writer.println("Saved Operating Periods: ${Measurer.savedOperatingPeriods}")
    writer.println("Saved Line Versions: ${Measurer.savedLineVersions}")
    writer.println("Saved Routes: ${Measurer.savedRoutes}")
    writer.println("-----------------------------------")
    writer.println("Searched Journeys: ${Measurer.searchedJourneys}")
    writer.println("Searched Scheduled Stops: ${Measurer.searchedScheduledStops}")
    writer.println("Searched Physical Stops: ${Measurer.searchedPhysicalStops}")
    writer.println("Searched Route Stops: ${Measurer.searchedRouteStops}")
    writer.println("Searched Operating Periods: ${Measurer.searchedOperatingPeriods}")
    writer.println("Searched Line Versions: ${Measurer.searchedLineVersions}")
    writer.println("Searched Routes: ${Measurer.searchedRoutes}")
    writer.println("-----------------------------------")
    writer.println("total time, XML parse, DB select time, DB save time")
    writer.println("${importTime.inWholeMilliseconds}, ${Measurer.xmlParse.inWholeMilliseconds}, " +
            "${Measurer.dbFind.inWholeMilliseconds}, ${Measurer.dbSave.inWholeMilliseconds}")
    writer.flush()
}
