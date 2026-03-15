package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.repository.dto.LineVersionMapDto
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface GetJourneysOperatingInDayUseCase {
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
        val arrival: ZonedDateTime?,
        val departure: ZonedDateTime?,
    )

    data class MapJourney(
        val relationalId: Long,
        val lineVersionId: Long,
        val routeId: Long?,
        val schedule: List<MapScheduledStop>,
        var nextDayFirstStopIndex: Int?,
    )

    data class JourneysOperatingInDayResult(
        val startingThisDay: List<MapJourney>,
        val continuingThisDay: List<MapJourney>,
        val routes: List<MapRoute>,
        val lineVersions: List<LineVersionMapDto>,
    )

    fun getJourneysOperatingInDay(day: LocalDate, timezone: ZoneId): JourneysOperatingInDayResult
}