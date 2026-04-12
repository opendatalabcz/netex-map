package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsParserPort
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsSourcePort
import cz.cvut.fit.gaierda1.domain.port.RoutingPort
import org.springframework.stereotype.Component

@Component
class EnrichStopsWithPositions: EnrichStopsWithPositionsUseCase {
    override fun enrichStopsWithPositions(
        jrUtilGtfsSourcePort: JrUtilGtfsSourcePort,
        jrUtilGtfsParserPort: JrUtilGtfsParserPort,
        addPositionToStopsByNameUseCase: AddPositionToStopsByNameUseCase,
        routingPort: RoutingPort,
    ) {
        val jrUtilGtfsStreamsIterator = jrUtilGtfsSourcePort.provideInput().iterator()
        val jrUtilGtfsParseResult = jrUtilGtfsParserPort.parseGtfs(jrUtilGtfsStreamsIterator)
        val positionAssignmentResults = addPositionToStopsByNameUseCase.addPositionToStopsByName(jrUtilGtfsParseResult)
    }
}
