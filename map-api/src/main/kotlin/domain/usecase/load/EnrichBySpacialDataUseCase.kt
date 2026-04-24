package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsSourcePort

interface EnrichBySpacialDataUseCase {
    fun enrichStopsWithPositions(
        jrUtilGtfsSourcePort: JrUtilGtfsSourcePort,
        jrUtilGtfsParserPort: JrUtilGtfsParserPort,
        normalizeStopNameUseCase: NormalizeStopNameUseCase,
        addJrUtilPositionToStopsByNameUseCase: AddJrUtilPositionToStopsByNameUseCase,
        calculateRoutesFromWaypointsUseCase: CalculateRoutesFromWaypointsUseCase,
    )
}
