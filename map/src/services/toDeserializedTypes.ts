import type { Journey, JourneyWithDatesAndTimes } from '@/api/model/journey'
import type { LineVersion, LineVersionWithDates } from '@/api/model/lineVersion'
import type { OperatingPeriod, OperatingPeriodWithDates } from '@/api/model/operatingPeriod'
import type { ScheduledStop, ScheduledStopWithTimes } from '@/api/model/scheduledStop'
import type {
    MapJourney,
    MapScheduledStop,
    MapJourneyWithDates,
    MapScheduledStopWithDates,
    MapRawRoute,
    MapRoute,
} from '@/api/model/journeysOperatingInFrame'
import type {
    WallScheduledStop,
    WallActivePeriod,
    WallLineVersion,
    WallOperatingPeriod,
    WallTimetable,
    WallScheduledStopWithTimes,
    WallActivePeriodWithDates,
    WallLineVersionWithDates,
    WallOperatingPeriodWithDates,
    WallTimetableWithDates,
} from '@/api/model/wallTimetable'
import LocalTime from '@/util/localTime'
import { Buffer } from 'buffer'
import { Geometry } from 'wkx'

function toLineVersionWithDates(lineVersion: LineVersion): LineVersionWithDates {
    return {
        relationalId: lineVersion.relationalId,
        publicCode: lineVersion.publicCode,
        name: lineVersion.name,
        shortName: lineVersion.shortName,
        transportMode: lineVersion.transportMode,
        validFrom: new Date(lineVersion.validFrom),
        validTo: new Date(lineVersion.validTo),
        isDetour: lineVersion.isDetour,
    }
}

function toOperatingPeriodWithDates(operatingPeriod: OperatingPeriod): OperatingPeriodWithDates {
    return {
        fromDate: new Date(operatingPeriod.fromDate),
        toDate: new Date(operatingPeriod.toDate),
        validDays: operatingPeriod.validDays,
    }
}

function toScheduledStopWithTimes(scheduledStop: ScheduledStop): ScheduledStopWithTimes {
    return {
        arrival: scheduledStop.arrival == null ? null : LocalTime.parse(scheduledStop.arrival),
        departure:
            scheduledStop.departure == null ? null : LocalTime.parse(scheduledStop.departure),
    }
}

function toJourneyWithDatesAndTimes(journey: Journey): JourneyWithDatesAndTimes {
    return {
        relationalId: journey.relationalId,
        journeyNumber: journey.journeyNumber,
        lineVersion: toLineVersionWithDates(journey.lineVersion),
        schedule: journey.schedule.map(toScheduledStopWithTimes),
        operatingPeriod: toOperatingPeriodWithDates(journey.operatingPeriod),
        route: journey.route,
        nextDayFirstStopIndex: journey.nextDayFirstStopIndex,
        beginTime: journey.beginTime,
        endTime: journey.endTime,
        timezone: journey.timezone,
    }
}

function toMapScheduledStopWithDates(
    mapScheduledStop: MapScheduledStop,
): MapScheduledStopWithDates {
    return {
        arrival: mapScheduledStop.arrival == null ? null : new Date(mapScheduledStop.arrival),
        departure: mapScheduledStop.departure == null ? null : new Date(mapScheduledStop.departure),
    }
}

function toMapJourneyWithDates(mapJourney: MapJourney): MapJourneyWithDates {
    return {
        relationalId: mapJourney.relationalId,
        lineVersionId: mapJourney.lineVersionId,
        routeId: mapJourney.routeId,
        schedule: mapJourney.schedule.map(toMapScheduledStopWithDates),
        nextDayFirstStopIndex: mapJourney.nextDayFirstStopIndex,
        fromPreviousDay: mapJourney.fromPreviousDay,
    }
}

function toMapRoute(route: MapRawRoute): MapRoute {
    const buffer = Buffer.from(route.pointSequence, 'base64')
    const geoJson = Geometry.parse(buffer).toGeoJSON()
    return {
        relationalId: route.relationalId,
        pointSequence: geoJson as GeoJSON.LineString,
        totalDistance: route.totalDistance,
        routeStops: route.routeStops,
    }
}

function toWallScheduledStopWithTimes(
    wallScheduledStop: WallScheduledStop,
): WallScheduledStopWithTimes {
    return {
        name: wallScheduledStop.name,
        stopOnRequest: wallScheduledStop.stopOnRequest,
        arrival:
            wallScheduledStop.arrival == null ? null : LocalTime.parse(wallScheduledStop.arrival),
        departure:
            wallScheduledStop.departure == null
                ? null
                : LocalTime.parse(wallScheduledStop.departure),
    }
}

function toWallActivePeriodWithDates(
    wallActivePeriod: WallActivePeriod,
): WallActivePeriodWithDates {
    return {
        fromDate: new Date(wallActivePeriod.fromDate),
        toDate: new Date(wallActivePeriod.toDate),
    }
}

function toWallLineVersionWithDates(wallLineVersion: WallLineVersion): WallLineVersionWithDates {
    return {
        relationalId: wallLineVersion.relationalId,
        publicCode: wallLineVersion.publicCode,
        name: wallLineVersion.name,
        shortName: wallLineVersion.shortName,
        transportMode: wallLineVersion.transportMode,
        isDetour: wallLineVersion.isDetour,
        activePeriods: wallLineVersion.activePeriods.map(toWallActivePeriodWithDates),
    }
}

function toWallOperatingPeriodWithDates(
    wallOperatingPeriod: WallOperatingPeriod,
): WallOperatingPeriodWithDates {
    return {
        operatingDays: wallOperatingPeriod.operatingDays,
        operationExceptions: new Map([
            [
                'ALSO_OPERATES',
                wallOperatingPeriod.operationExceptions.ALSO_OPERATES.map((day) => new Date(day)),
            ],
            [
                'DOES_NOT_OPERATE',
                wallOperatingPeriod.operationExceptions.DOES_NOT_OPERATE.map(
                    (day) => new Date(day),
                ),
            ],
        ]),
        journeys: new Map(
            Object.entries(wallOperatingPeriod.journeys).map(([id, stops]) => [
                Number.parseInt(id),
                stops.map(toWallScheduledStopWithTimes),
            ]),
        ),
    }
}

function toWallTimetableWithDates(wallTimetable: WallTimetable): WallTimetableWithDates {
    return {
        lineVersion: toWallLineVersionWithDates(wallTimetable.lineVersion),
        operatingPeriods: wallTimetable.operatingPeriods.map(toWallOperatingPeriodWithDates),
    }
}

export {
    toLineVersionWithDates,
    toOperatingPeriodWithDates,
    toScheduledStopWithTimes,
    toJourneyWithDatesAndTimes,
    toMapScheduledStopWithDates,
    toMapJourneyWithDates,
    toMapRoute,
    toWallScheduledStopWithTimes,
    toWallActivePeriodWithDates,
    toWallOperatingPeriodWithDates,
    toWallTimetableWithDates,
}
