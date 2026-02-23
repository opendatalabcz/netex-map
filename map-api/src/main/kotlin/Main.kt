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
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import java.io.File
import java.time.LocalTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)
    doImport(appContext, true)
}

fun doImport(appContext: ConfigurableApplicationContext, useDomainImport: Boolean) {
    val entityManagerFactory = appContext.getBean(EntityManagerFactory::class.java)
    val sessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
    val stats = sessionFactory.statistics

    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder)

    val timetableParser = appContext.getBean(TimetableParser::class.java)
    val calculateNextDayOperation = appContext.getBean(CalculateNextDayOperationUseCase::class.java)
    val calculateJourneyRoutes = appContext.getBean(CalculateJourneyRoutesMock::class.java)
    val importTimetable = appContext.getBean(ImportTimetables::class.java)

    val timetableDataParser = appContext.getBean(TimetableDataParser::class.java)
    val calculateNextDayOperationData = appContext.getBean(CalculateNextDayOperationData::class.java)
    val calculateJourneyRoutesData = appContext.getBean(CalculateJourneyRoutesDataMock::class.java)
    val importTimetablesData = appContext.getBean(ImportTimetablesData::class.java)

    val start = LocalTime.now()
    println("$start: Begin importing timetables")
    if (useDomainImport) {
        importTimetable.importTimetables(timetableSource, timetableParser, calculateNextDayOperation, calculateJourneyRoutes)
    } else {
        importTimetablesData.importTimetables(timetableSource, timetableDataParser, calculateNextDayOperationData, calculateJourneyRoutesData)
    }
    val end = LocalTime.now()
    println("$end: Done importing timetables in ${end.toSecondOfDay() - start.toSecondOfDay()}s")

    println("Query count: ${stats.queryExecutionCount}")
    println("Insert count: ${stats.entityInsertCount}")
    println("Update count: ${stats.entityUpdateCount}")
    println("Prepare statement count: ${stats.prepareStatementCount}")
    println("Fetch count: ${stats.entityFetchCount}")
    println("Load count: ${stats.entityLoadCount}")
}
