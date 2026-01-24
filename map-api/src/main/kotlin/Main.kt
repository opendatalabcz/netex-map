package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.netex.assembler.NetexTimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File
import java.time.LocalDateTime

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val appContext = runApplication<Application>(*args)

    val xmlFilesFolder = File("src/data/public_scheduled_transport")
    val timetableSource = TimetableDirectorySource(xmlFilesFolder)
    val timetableParser = appContext.getBean(NetexTimetableParser::class.java)
    val importTimetableUseCase = appContext.getBean(ImportTimetablesUseCase::class.java)
    println("${LocalDateTime.now()}: Begin importing timetables")
    importTimetableUseCase.importTimetables(timetableSource, timetableParser)
    println("${LocalDateTime.now()}: Done importing timetables")
}
