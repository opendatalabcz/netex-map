package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsParseResult

interface AddJrUtilPositionToStopsByNameUseCase {
    fun addPositionToStopsByName(jrUtilGtfsParseResult: JrUtilGtfsParseResult): Map<Long, JrUtilGtfsParserPort.JrUtilGtfsStopParseResult>
}
