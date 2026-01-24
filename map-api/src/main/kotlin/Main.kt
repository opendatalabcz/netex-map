package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.data.TimetableDataParser
import cz.cvut.fit.gaierda1.data.netex.domain.TimetableDomainParser
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File
import java.time.LocalTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)

    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder)
    val timetableDomainParser = appContext.getBean(TimetableDomainParser::class.java)
    val importTimetableUseCase = appContext.getBean(ImportTimetablesUseCase::class.java)

    val start = LocalTime.now()
    println("$start: Begin importing timetables")
    if (true) {
        importTimetableUseCase.importTimetables(timetableSource, timetableDomainParser)
    } else {
        val timetableDataParser = appContext.getBean(TimetableDataParser::class.java)
        timetableSource.provideInput { entryContentStream -> timetableDataParser.parseTimetable(entryContentStream) }
    }
    val end = LocalTime.now()
    println("$end: Done importing timetables in ${end.toSecondOfDay() - start.toSecondOfDay()}s")
}
