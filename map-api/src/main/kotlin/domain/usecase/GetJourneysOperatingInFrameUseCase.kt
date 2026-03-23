package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionMapDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

interface GetJourneysOperatingInFrameUseCase {
    data class MapRouteStop(
        val pointSequenceIndex: Int,
        val distanceToNextStop: Double,
    )

    data class MapRoute(
        val relationalId: Long,
        val pointSequence: ByteArray,
        val totalDistance: Double,
        val routeStops: List<MapRouteStop>,
    )

    data class MapScheduledStop(
        val arrival: OffsetDateTime?,
        val departure: OffsetDateTime?,
    )

    data class MapJourney(
        val relationalId: Long,
        val lineVersionId: Long,
        val routeId: Long?,
        val schedule: List<MapScheduledStop>,
        val nextDayFirstStopIndex: Int?,
    )

    data class JourneysOperatingInFrameResult(
        val startingThisDay: List<MapJourney>,
        val continuingThisDay: List<MapJourney>,
        val routes: List<MapRoute>,
        val lineVersions: List<LineVersionMapDto>,
    )

    fun getJourneysOperatingInFrame(
        lonMin: Double,
        latMin: Double,
        lonMax: Double,
        latMax: Double,
        zoomLevel: Int,
        day: OffsetDateTime,
    ): JourneysOperatingInFrameResult
}