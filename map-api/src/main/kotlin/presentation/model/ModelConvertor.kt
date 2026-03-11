package cz.cvut.fit.gaierda1.presentation.model

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInDayUseCase
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class ModelConvertor {
    fun toHttp(position: Coordinate, latitudeFirst: Boolean): Array<Double> =
        if (latitudeFirst) arrayOf(position.y, position.x) else arrayOf(position.x, position.y)

    fun toHttp(physicalStop: PhysicalStop, latitudeFirst: Boolean): HttpPhysicalStop = HttpPhysicalStop(
        relationalId = physicalStop.relationalId,
        externalId = physicalStop.externalId,
        name = physicalStop.name,
        position = toHttp(physicalStop.position.coordinate, latitudeFirst),
        tags = physicalStop.tags,
    )

    fun toHttp(routeStop: RouteStop, latitudeFirst: Boolean): HttpRouteStop = HttpRouteStop(
        physicalStop = toHttp(routeStop.physicalStop, latitudeFirst),
        pointSequenceIndex = routeStop.pointSequenceIndex,
    )

    fun toHttp(route: Route, latitudeFirst: Boolean): HttpRoute = HttpRoute(
        relationalId = route.relationalId,
        externalId = route.externalId,
        pointSequence = route.pointSequence.coordinates.map { toHttp(it, latitudeFirst) },
        routeStops = route.routeStops.map { toHttp(it, latitudeFirst) }
    )

    fun toHttp(operatingPeriod: OperatingPeriod): HttpOperatingPeriod = HttpOperatingPeriod(
        fromDate = ZonedDateTime.of(operatingPeriod.fromDate, operatingPeriod.timezone),
        toDate = ZonedDateTime.of(operatingPeriod.toDate, operatingPeriod.timezone),
        validDays = operatingPeriod.validDays.map { bit -> if (bit) '1' else '0' }.joinToString(""),
    )

    fun toHttp(lineVersion: LineVersion): HttpLineVersion = HttpLineVersion(
        relationalId = lineVersion.relationalId,
        externalId = lineVersion.externalId,
        publicCode = lineVersion.publicCode,
        name = lineVersion.name,
        shortName = lineVersion.shortName,
        transportMode = lineVersion.transportMode,
        validFrom = ZonedDateTime.of(lineVersion.validFrom, lineVersion.timezone),
        validTo = ZonedDateTime.of(lineVersion.validTo, lineVersion.timezone),
        isDetour = lineVersion.isDetour,
    )

    fun toHttp(scheduledStop: ScheduledStop): HttpScheduledStop = HttpScheduledStop(
        name = scheduledStop.name,
        stopOnRequest = scheduledStop.stopOnRequest,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    fun toHttp(journey: Journey, latitudeFirst: Boolean): HttpJourney = HttpJourney(
        relationalId = journey.relationalId,
        externalId = journey.externalId,
        lineVersion = toHttp(journey.lineVersion),
        journeyPatternId = journey.journeyPatternId,
        schedule = journey.schedule.map(::toHttp),
        operatingPeriods = journey.operatingPeriods.map(::toHttp),
        route = journey.route?.let { toHttp(it, latitudeFirst) },
        nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
    )

    fun toHttp(daySpecificScheduledStop: GetJourneysOperatingInDayUseCase.DaySpecificScheduledStop): HttpDaySpecificScheduledStop = HttpDaySpecificScheduledStop(
        name = daySpecificScheduledStop.name,
        stopOnRequest = daySpecificScheduledStop.stopOnRequest,
        arrival = daySpecificScheduledStop.arrival,
        departure = daySpecificScheduledStop.departure,
    )

    fun toHttp(daySpecificJourney: GetJourneysOperatingInDayUseCase.DaySpecificJourney): HttpDaySpecificJourney = HttpDaySpecificJourney(
        relationalId = daySpecificJourney.relationalId,
        lineVersion = toHttp(daySpecificJourney.lineVersion),
        routeId = daySpecificJourney.routeId,
        schedule = daySpecificJourney.schedule.map(::toHttp),
        nextDayFirstStopIndex = daySpecificJourney.nextDayFirstStopIndex,
    )

    fun toHttp(
        journeysOperatingInDayResult: GetJourneysOperatingInDayUseCase.JourneysOperatingInDayResult,
        latitudeFirst: Boolean,
    ): HttpJourneysOperatingInDayResult = HttpJourneysOperatingInDayResult(
        startingThisDay = journeysOperatingInDayResult.startingThisDay.map(::toHttp),
        continuingThisDay = journeysOperatingInDayResult.continuingThisDay.map(::toHttp),
        routes = journeysOperatingInDayResult.routes.map { toHttp(it, latitudeFirst) },
    )
}