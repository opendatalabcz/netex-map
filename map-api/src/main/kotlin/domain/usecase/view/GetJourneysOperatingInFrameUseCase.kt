package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.model.LineType
import java.time.OffsetDateTime

interface GetJourneysOperatingInFrameUseCase {
    data class FrameScheduledStop(
        val arrival: OffsetDateTime?,
        val departure: OffsetDateTime?,
    )

    data class FrameJourney(
        val relationalId: Long,
        val lineVersionId: Long,
        val routeId: Long,
        val schedule: List<FrameScheduledStop>,
        val nextDayFirstStopIndex: Int?,
        val fromPreviousDay: Boolean,
    )

    data class FrameLineVersion(
        val relationalId: Long,
        val lineType: LineType,
    )

    data class JourneysOperatingInFrameResult(
        val journeys: List<FrameJourney>,
        val routes: List<GetEncodedRoutesUseCase.EncodedRoute>,
        val lineVersions: List<FrameLineVersion>,
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
