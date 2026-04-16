package cz.cvut.fit.gaierda1.domain.usecase.view

import java.time.OffsetDateTime

interface GetJourneysOperatingInFrameUseCase {
    data class MapScheduledStop(
        val arrival: OffsetDateTime?,
        val departure: OffsetDateTime?,
    )

    data class MapJourney(
        val relationalId: Long,
        val lineVersionId: Long,
        val routeId: Long,
        val schedule: List<MapScheduledStop>,
        val nextDayFirstStopIndex: Int?,
        val fromPreviousDay: Boolean,
    )

    data class JourneysOperatingInFrameResult(
        val journeys: List<MapJourney>,
        val routes: List<GetEncodedRoutesUseCase.EncodedRoute>,
    )

    fun getJourneysOperatingInFrame(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        zoomLevel: Int,
        dateTime: OffsetDateTime,
        excludedJourneyIds: Set<Long>,
        excludedJourneyIdsFromPreviousDay: Set<Long>,
        excludedRouteIds: Set<Long>,
    ): JourneysOperatingInFrameResult
}
