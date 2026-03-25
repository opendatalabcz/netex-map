package cz.cvut.fit.gaierda1.presentation.model

import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionMapDto
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInFrameUseCase.*

data class HttpMapRoute(
    val relationalId: Long,
    val pointSequence: String,
    val totalDistance: Double,
    val routeStops: List<Double>,
)

data class HttpJourneysOperatingInDayResult(
    val journeys: List<MapJourney>,
    val routes: List<HttpMapRoute>,
    val lineVersions: List<LineVersionMapDto>,
)
