package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsParseResult

interface AddJrUtilPositionToStopsByNameUseCase {
    data class AddPositionToStopsByNameResult(
        val linePublicCode: String,
        val assignmentsByStopId: Map<Long, PhysicalStop>,
    )
    fun addPositionToStopsByName(jrUtilGtfsParseResult: JrUtilGtfsParseResult): List<AddPositionToStopsByNameResult>
}
