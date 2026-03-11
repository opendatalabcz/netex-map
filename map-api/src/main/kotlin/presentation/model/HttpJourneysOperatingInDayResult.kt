package cz.cvut.fit.gaierda1.presentation.model

import java.time.ZonedDateTime

data class HttpDaySpecificScheduledStop(
    val name: String,
    val stopOnRequest: Boolean,
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?,
)

data class HttpDaySpecificJourney(
    val relationalId: Long,
    val lineVersion: HttpLineVersion,
    val routeId: Long?,
    val schedule: List<HttpDaySpecificScheduledStop>,
    var nextDayFirstStopIndex: Int?,
)

data class HttpJourneysOperatingInDayResult(
    val startingThisDay: List<HttpDaySpecificJourney>,
    val continuingThisDay: List<HttpDaySpecificJourney>,
    val routes: List<HttpRoute>,
)
