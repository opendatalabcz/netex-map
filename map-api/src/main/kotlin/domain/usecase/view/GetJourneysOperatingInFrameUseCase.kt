package cz.cvut.fit.gaierda1.domain.usecase.view

import java.time.OffsetDateTime

interface GetJourneysOperatingInFrameUseCase {
    data class MapRoute(
        val relationalId: Long,
        val pointSequence: ByteArray,
        val totalDistance: Double,
        val routeStops: List<Double>,
    )

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
        val routes: List<MapRoute>,
    )

    fun getJourneysOperatingInFrame(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        zoomLevel: Int,
        dateTime: OffsetDateTime,
    ): JourneysOperatingInFrameResult
}
