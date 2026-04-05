package cz.cvut.fit.gaierda1.presentation.model

import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneysOperatingInFrameUseCase.*

data class HttpMapRoute(
    val relationalId: Long,
    val pointSequence: String,
    val totalDistance: Double,
    val routeStops: List<Double>,
)

data class HttpJourneysOperatingInFrameResult(
    val journeys: List<MapJourney>,
    val routes: List<HttpMapRoute>,
)
