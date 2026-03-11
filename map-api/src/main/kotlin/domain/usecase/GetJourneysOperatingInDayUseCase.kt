package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.Route
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface GetJourneysOperatingInDayUseCase {
    data class DaySpecificScheduledStop(
        val name: String,
        val stopOnRequest: Boolean,
        val arrival: ZonedDateTime?,
        val departure: ZonedDateTime?,
    )

    data class DaySpecificJourney(
        val relationalId: Long,
        val lineVersion: LineVersion,
        val routeId: Long?,
        val schedule: List<DaySpecificScheduledStop>,
        var nextDayFirstStopIndex: Int?,
    )

    data class JourneysOperatingInDayResult(
        val startingThisDay: List<DaySpecificJourney>,
        val continuingThisDay: List<DaySpecificJourney>,
        val routes: List<Route>,
    )

    fun getJourneysOperatingInDay(day: LocalDate, timezone: ZoneId): JourneysOperatingInDayResult
}