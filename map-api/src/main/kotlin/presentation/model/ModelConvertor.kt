package cz.cvut.fit.gaierda1.presentation.model

import cz.cvut.fit.gaierda1.data.orm.model.ActivePeriod
import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInFrameUseCase
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Component
import kotlin.io.encoding.Base64

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
        routeFraction = routeStop.routeFraction,
    )

    fun toHttp(route: Route, latitudeFirst: Boolean): HttpRoute = HttpRoute(
        relationalId = route.relationalId,
        externalId = route.externalId,
        pointSequence = route.pointSequence.coordinates.map { toHttp(it, latitudeFirst) },
        routeStops = route.routeStops.map { toHttp(it, latitudeFirst) },
        totalDistance = route.totalDistance,
    )

    fun toHttp(operatingPeriod: OperatingPeriod): HttpOperatingPeriod = HttpOperatingPeriod(
        fromDate = operatingPeriod.fromDate,
        toDate = operatingPeriod.toDate,
        validDays = operatingPeriod.validDays.map { bit -> if (bit) '1' else '0' }.joinToString(""),
    )

    fun toHttp(activePeriod: ActivePeriod): HttpActivePeriod = HttpActivePeriod(
        fromDate = activePeriod.periodId.fromDate,
        toDate = activePeriod.toDate,
    )

    fun toHttp(lineVersion: LineVersion): HttpLineVersion = HttpLineVersion(
        relationalId = lineVersion.relationalId,
        publicCode = lineVersion.publicCode,
        name = lineVersion.name,
        shortName = lineVersion.shortName,
        transportMode = lineVersion.transportMode,
        validFrom = lineVersion.validFrom,
        validTo = lineVersion.validTo,
        isDetour = lineVersion.isDetour,
        activePeriods = lineVersion.activePeriods.map(::toHttp),
    )

    fun toHttp(scheduledStop: ScheduledStop): HttpScheduledStop = HttpScheduledStop(
        name = scheduledStop.name,
        stopOnRequest = scheduledStop.stopOnRequest,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    fun toHttp(journey: Journey, includeRoute: Boolean, latitudeFirst: Boolean): HttpJourney = HttpJourney(
        relationalId = journey.relationalId,
        externalId = journey.externalId,
        lineVersion = toHttp(journey.lineVersion),
        journeyPatternId = journey.journeyPatternId,
        schedule = journey.schedule.map(::toHttp),
        operatingPeriod = toHttp(journey.operatingPeriod),
        route = if (includeRoute) journey.route?.let { toHttp(it, latitudeFirst) } else null,
        nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
        beginTime = journey.beginTime,
        endTime = journey.endTime,
        timezone = journey.timezone,
    )

    fun toHttp(route: GetJourneysOperatingInFrameUseCase.MapRoute): HttpMapRoute = HttpMapRoute(
        relationalId = route.relationalId,
        pointSequence = Base64.encode(route.pointSequence),
        totalDistance = route.totalDistance,
        routeStops = route.routeStops,
    )

    fun toHttp(
        journeysOperatingInFrameResult: GetJourneysOperatingInFrameUseCase.JourneysOperatingInFrameResult,
    ): HttpJourneysOperatingInDayResult = HttpJourneysOperatingInDayResult(
        journeys = journeysOperatingInFrameResult.journeys,
        routes = journeysOperatingInFrameResult.routes.map(::toHttp),
        lineVersions = journeysOperatingInFrameResult.lineVersions,
    )
}