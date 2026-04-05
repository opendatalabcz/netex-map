package cz.cvut.fit.gaierda1.presentation.model

import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneysOperatingInFrameUseCase
import org.springframework.stereotype.Component
import kotlin.io.encoding.Base64

@Component
class ModelConvertor {
    fun toHttp(route: GetJourneysOperatingInFrameUseCase.MapRoute): HttpMapRoute = HttpMapRoute(
        relationalId = route.relationalId,
        pointSequence = Base64.encode(route.pointSequence),
        totalDistance = route.totalDistance,
        routeStops = route.routeStops,
    )

    fun toHttp(
        journeysOperatingInFrameResult: GetJourneysOperatingInFrameUseCase.JourneysOperatingInFrameResult,
    ): HttpJourneysOperatingInFrameResult = HttpJourneysOperatingInFrameResult(
        journeys = journeysOperatingInFrameResult.journeys,
        routes = journeysOperatingInFrameResult.routes.map(::toHttp),
    )
}
