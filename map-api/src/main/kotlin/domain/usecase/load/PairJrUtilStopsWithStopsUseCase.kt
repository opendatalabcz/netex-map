package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort.JrUtilGtfsParseResult

interface PairJrUtilStopsWithStopsUseCase {
    data class PairJrUtilStopsWithStopsResult(
        val linePublicCode: String,
        val assignmentsByStopId: Map<Long, PhysicalStop>,
    )
    fun pairJrUtilStopsWithStops(
        jrUtilGtfsParseResult: JrUtilGtfsParseResult,
        normalizeStopNameUseCase: NormalizeStopNameUseCase,
    ): List<PairJrUtilStopsWithStopsResult>
}
