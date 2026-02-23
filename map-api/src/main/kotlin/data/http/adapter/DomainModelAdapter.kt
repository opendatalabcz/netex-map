package cz.cvut.fit.gaierda1.data.http.adapter

import cz.cvut.fit.gaierda1.data.http.model.HttpJourney
import cz.cvut.fit.gaierda1.data.http.model.HttpLineVersion
import cz.cvut.fit.gaierda1.data.http.model.HttpOperatingPeriod
import cz.cvut.fit.gaierda1.data.http.model.HttpPhysicalStop
import cz.cvut.fit.gaierda1.data.http.model.HttpRoute
import cz.cvut.fit.gaierda1.data.http.model.HttpRouteStop
import cz.cvut.fit.gaierda1.data.http.model.HttpScheduledStop
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.Point
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteStop
import cz.cvut.fit.gaierda1.domain.model.ScheduledStop
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class DomainModelAdapter {
    fun toHttp(point: Point): Array<Double> = arrayOf(point.longitude, point.latitude)

    fun toHttp(physicalStop: PhysicalStop): HttpPhysicalStop = HttpPhysicalStop(
        stopId = physicalStop.stopId.value,
        name = physicalStop.name,
        position = toHttp(physicalStop.position),
        tags = physicalStop.tags,
    )

    fun toHttp(routeStop: RouteStop): HttpRouteStop = HttpRouteStop(
        physicalStop = toHttp(routeStop.physicalStop),
        pointSequenceIndex = routeStop.pointSequenceIndex,
    )

    fun toHttp(route: Route): HttpRoute = HttpRoute(
        routeId = route.routeId.value,
        pointSequence = route.pointSequence.map(::toHttp),
        routeStops = route.routeStops.map(::toHttp),
    )

    fun toHttp(operatingPeriod: OperatingPeriod): HttpOperatingPeriod = HttpOperatingPeriod(
        fromDate = ZonedDateTime.of(operatingPeriod.fromDate, operatingPeriod.timezone),
        toDate = ZonedDateTime.of(operatingPeriod.toDate, operatingPeriod.timezone),
        validDays = operatingPeriod.validDays.map { if (it) 1 else 0 }.joinToString(""),
    )

    fun toHttp(lineVersion: LineVersion): HttpLineVersion = HttpLineVersion(
        lineId = lineVersion.lineId.value,
        publicCode = lineVersion.publicCode,
        name = lineVersion.name,
        shortName = lineVersion.shortName,
        transportMode = lineVersion.transportMode,
        validFrom = ZonedDateTime.of(lineVersion.validIn.from, lineVersion.validIn.timezone),
        validTo = ZonedDateTime.of(lineVersion.validIn.to, lineVersion.validIn.timezone),
        isDetour = lineVersion.isDetour,
    )

    fun toHttp(scheduledStop: ScheduledStop): HttpScheduledStop = HttpScheduledStop(
        name = scheduledStop.name,
        stopOnRequest = scheduledStop.stopOnRequest,
        arrival = scheduledStop.arrival,
        departure = scheduledStop.departure,
    )

    fun toHttp(journey: Journey): HttpJourney = HttpJourney(
        journeyId = journey.journeyId.value,
        lineVersion = toHttp(journey.lineVersion),
        journeyPatternId = journey.journeyPatternId.value,
        schedule = journey.schedule.map(::toHttp),
        operatingPeriods = journey.operatingPeriods.map(::toHttp),
        route = journey.route?.let(::toHttp),
        nextDayFirstStopIndex = journey.nextDayFirstStopIndex,
    )
}