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
} from '@/api/model/journeysOperatingInDay'
import LocalTime from '@/util/localTime'
import { Buffer } from 'buffer'
import { Geometry } from 'wkx'

function toLineVersionWithDates(lineVersion: LineVersion): LineVersionWithDates {
    return {
        relationalId: lineVersion.relationalId,
        externalId: lineVersion.externalId,
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
        name: scheduledStop.name,
        stopOnRequest: scheduledStop.stopOnRequest,
        arrival: scheduledStop.arrival == null ? null : LocalTime.parse(scheduledStop.arrival),
        departure:
            scheduledStop.departure == null ? null : LocalTime.parse(scheduledStop.departure),
    }
}

function toJourneyWithDatesAndTimes(journey: Journey): JourneyWithDatesAndTimes {
    return {
        relationalId: journey.relationalId,
        externalId: journey.externalId,
        lineVersion: toLineVersionWithDates(journey.lineVersion),
        journeyPatternId: journey.journeyPatternId,
        schedule: journey.schedule.map(toScheduledStopWithTimes),
        operatingPeriods: journey.operatingPeriods.map(toOperatingPeriodWithDates),
        route: journey.route,
        nextDayFirstStopIndex: journey.nextDayFirstStopIndex,
    }
}

function toMapScheduledStopWithDates(mapScheduledStop: MapScheduledStop): MapScheduledStopWithDates {
    return {
        arrival:
            mapScheduledStop.arrival == null
                ? null
                : new Date(mapScheduledStop.arrival),
        departure:
            mapScheduledStop.departure == null
                ? null
                : new Date(mapScheduledStop.departure),
    }
}

function toMapJourneyWithDates(mapJourney: MapJourney): MapJourneyWithDates {
    return {
        relationalId: mapJourney.relationalId,
        lineVersionId: mapJourney.lineVersionId,
        routeId: mapJourney.routeId,
        schedule: mapJourney.schedule.map(toMapScheduledStopWithDates),
        nextDayFirstStopIndex: mapJourney.nextDayFirstStopIndex,
    }
}

function toMapRoute(route: MapRawRoute): MapRoute {
    const buffer = Buffer.from(route.pointSequence, 'base64')
    const geoJson = Geometry.parse(buffer).toGeoJSON();
    return {
        relationalId: route.relationalId,
        pointSequence: geoJson as GeoJSON.LineString,
        totalDistance: route.totalDistance,
        routeStops: route.routeStops,
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
}
