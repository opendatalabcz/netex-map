package cz.cvut.fit.gaierda1.cmd

import cz.cvut.fit.gaierda1.data.filesystem.TimetableDirectorySource
import cz.cvut.fit.gaierda1.data.filesystem.TimetableZipFileSource
import cz.cvut.fit.gaierda1.data.netex.TimetableParser
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateLineVersionActivePeriodsUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.CalculateNextDayOperationUseCase
import cz.cvut.fit.gaierda1.domain.usecase.load.ImportTimetablesUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.File
import kotlin.time.measureTime

@Component
@Order(0)
class ImportNetexDataProcedure(
    private val timetableParser: TimetableParser,
    private val calculateNextDayOperationUseCase: CalculateNextDayOperationUseCase,
    private val calculateLineVersionActivePeriodsUseCase: CalculateLineVersionActivePeriodsUseCase,
    private val importTimetablesUseCase: ImportTimetablesUseCase,
): CommandLineRunner {
    companion object {
        private const val COMMON_ARGUMENT_PREFIX = "--import"
        private const val ZIP_ARGUMENT_PREFIX = "$COMMON_ARGUMENT_PREFIX-zip"
    }

    private val log = LoggerFactory.getLogger(ImportNetexDataProcedure::class.java)

    override fun run(vararg args: String) {
        val importArgs = args.filter { it.startsWith(COMMON_ARGUMENT_PREFIX) }
        if (importArgs.isEmpty()) return
        log.info("Begin importing timetables")
        val importTime = measureTime {
            for (importArg in importArgs) {
                val importPath = importArg
                    .substringAfter("=")
                    .ifBlank { null }
                if (importPath == null) {
                    log.error("Missing import file/directory path after $COMMON_ARGUMENT_PREFIX argument")
                    continue
                }
                val importFile = File(importPath)
                if (!importFile.exists()) {
                    log.error("Import file/directory path does not exist: ${importFile.absolutePath}")
                    continue
                }
                val useZip = importArg.substringBefore("=") == ZIP_ARGUMENT_PREFIX
                if (useZip && !importFile.isFile) {
                    log.error("The path after $ZIP_ARGUMENT_PREFIX argument must denote a regular file")
                    continue
                } else if (!useZip && !importFile.isDirectory) {
                    log.error("The path after $COMMON_ARGUMENT_PREFIX argument must denote a directory")
                    continue
                }
                log.info("Importing from ${if (useZip) "file" else "directory"}: ${importFile.absolutePath}")
                val timetableSource =
                    if (useZip) TimetableZipFileSource(importFile.inputStream())
                    else TimetableDirectorySource(importFile)
                importTimetablesUseCase.importTimetables(
                    timetableSource,
                    timetableParser,
                    calculateNextDayOperationUseCase,
                )
            }
            calculateLineVersionActivePeriodsUseCase.calculateActivePeriods()
        }
        log.info("Done importing timetables in $importTime")
    }
}
