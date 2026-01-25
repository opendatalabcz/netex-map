package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.data.TimetableDataParser
import cz.cvut.fit.gaierda1.data.netex.domain.TimetableDomainParser
import cz.cvut.fit.gaierda1.domain.usecase.ImportDataTimetableUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ImportDomainTimetablesUseCase
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File
import java.time.LocalTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)

    val entityManagerFactory = appContext.getBean(EntityManagerFactory::class.java)
    val sessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
    val stats = sessionFactory.statistics

    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder)
    val timetableDomainParser = appContext.getBean(TimetableDomainParser::class.java)
    val importDomainTimetableUseCase = appContext.getBean(ImportDomainTimetablesUseCase::class.java)

    val timetableDataParser = appContext.getBean(TimetableDataParser::class.java)
    val importDataTimetableUseCase = appContext.getBean(ImportDataTimetableUseCase::class.java)

    val start = LocalTime.now()
    println("$start: Begin importing timetables")
    if (false) {
        importDomainTimetableUseCase.importTimetables(timetableSource, timetableDomainParser)
    } else {
        importDataTimetableUseCase.importTimetables(timetableSource, timetableDataParser)
    }
    val end = LocalTime.now()
    println("$end: Done importing timetables in ${end.toSecondOfDay() - start.toSecondOfDay()}s")

    println("Insert count: ${stats.entityInsertCount}")
    println("Select count: ${stats.queryExecutionCount}")
    println("Update count: ${stats.entityUpdateCount}")
    println("Prepare statement count: ${stats.prepareStatementCount}")
}
