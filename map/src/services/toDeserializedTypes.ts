import type {
    JourneyDetails,
    JourneyDetailsScheduledStop,
    JourneyDetailsScheduledStopWithTimes,
    JourneyDetailsWithTimes,
} from '@/api/model/journeyDetails'
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
    WallJourney,
    WallJourneyWithTimes,
} from '@/api/model/wallTimetable'
import LocalTime from '@/util/localTime'
import { Buffer } from 'buffer'
import { Geometry } from 'wkx'

export function toMapScheduledStopWithDates(
    mapScheduledStop: MapScheduledStop,
): MapScheduledStopWithDates {
    return {
        arrival: mapScheduledStop.arrival == null ? null : new Date(mapScheduledStop.arrival),
        departure: mapScheduledStop.departure == null ? null : new Date(mapScheduledStop.departure),
    }
}

export function toMapJourneyWithDates(mapJourney: MapJourney): MapJourneyWithDates {
    return {
        relationalId: mapJourney.relationalId,
        lineVersionId: mapJourney.lineVersionId,
        routeId: mapJourney.routeId,
        schedule: mapJourney.schedule.map(toMapScheduledStopWithDates),
        nextDayFirstStopIndex: mapJourney.nextDayFirstStopIndex,
        fromPreviousDay: mapJourney.fromPreviousDay,
    }
}

export function toMapRoute(route: MapRawRoute): MapRoute {
    const buffer = Buffer.from(route.pointSequence, 'base64')
    const geoJson = Geometry.parse(buffer).toGeoJSON()
    return {
        relationalId: route.relationalId,
        pointSequence: geoJson as GeoJSON.LineString,
        totalDistance: route.totalDistance,
        routeStops: route.routeStops,
    }
}

export function toWallScheduledStopWithTimes(
    wallScheduledStop: WallScheduledStop,
): WallScheduledStopWithTimes {
    return {
        arrival:
            wallScheduledStop.arrival == null ? null : LocalTime.parse(wallScheduledStop.arrival),
        departure:
            wallScheduledStop.departure == null
                ? null
                : LocalTime.parse(wallScheduledStop.departure),
    }
}

export function toWallActivePeriodWithDates(
    wallActivePeriod: WallActivePeriod,
): WallActivePeriodWithDates {
    return {
        fromDate: new Date(wallActivePeriod.fromDate),
        toDate: new Date(wallActivePeriod.toDate),
    }
}

export function toWallLineVersionWithDates(
    wallLineVersion: WallLineVersion,
): WallLineVersionWithDates {
    return {
        relationalId: wallLineVersion.relationalId,
        publicCode: wallLineVersion.publicCode,
        name: wallLineVersion.name,
        shortName: wallLineVersion.shortName,
        transportMode: wallLineVersion.transportMode,
        lineType: wallLineVersion.lineType,
        isDetour: wallLineVersion.isDetour,
        operator: wallLineVersion.operator,
        activePeriods: wallLineVersion.activePeriods.map(toWallActivePeriodWithDates),
        tariffStops: wallLineVersion.tariffStops,
        stops: wallLineVersion.stops,
    }
}

export function toWallJourneyWithDates(wallJourney: WallJourney): WallJourneyWithTimes {
    return {
        relationalId: wallJourney.relationalId,
        schedule: wallJourney.schedule.map(toWallScheduledStopWithTimes),
        requiresOrdering: wallJourney.requiresOrdering,
        baggageStorage: wallJourney.baggageStorage,
        cyclesAllowed: wallJourney.cyclesAllowed,
        lowFloorAccess: wallJourney.lowFloorAccess,
        reservationCompulsory: wallJourney.reservationCompulsory,
        reservationPossible: wallJourney.reservationPossible,
        snacksOnBoard: wallJourney.snacksOnBoard,
        unaccompaniedMinorAssistance: wallJourney.unaccompaniedMinorAssistance,
    }
}

export function toWallOperatingPeriodWithDates(
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
        journeys: wallOperatingPeriod.journeys.map(toWallJourneyWithDates),
    }
}

export function toWallTimetableWithDates(wallTimetable: WallTimetable): WallTimetableWithDates {
    return {
        lineVersion: toWallLineVersionWithDates(wallTimetable.lineVersion),
        operatingPeriods: wallTimetable.operatingPeriods.map(toWallOperatingPeriodWithDates),
        journeyPatterns: wallTimetable.journeyPatterns,
    }
}

export function toJourneyDetailsScheduledStopWithTimes(
    journeyDetailsScheduledStop: JourneyDetailsScheduledStop,
): JourneyDetailsScheduledStopWithTimes {
    return {
        arrival:
            journeyDetailsScheduledStop.arrival == null
                ? null
                : LocalTime.parse(journeyDetailsScheduledStop.arrival),
        departure:
            journeyDetailsScheduledStop.departure == null
                ? null
                : LocalTime.parse(journeyDetailsScheduledStop.departure),
        distanceToNextStop: journeyDetailsScheduledStop.distanceToNextStop,
        forBoarding: journeyDetailsScheduledStop.forBoarding,
        forAlighting: journeyDetailsScheduledStop.forAlighting,
        requiresOrdering: journeyDetailsScheduledStop.requiresOrdering,
        stopOnRequest: journeyDetailsScheduledStop.stopOnRequest,
        tariffZone: journeyDetailsScheduledStop.tariffZone,
        name: journeyDetailsScheduledStop.name,
        bistro: journeyDetailsScheduledStop.bistro,
        borderCrossing: journeyDetailsScheduledStop.borderCrossing,
        displaysForVisuallyImpaired: journeyDetailsScheduledStop.displaysForVisuallyImpaired,
        lowFloorAccess: journeyDetailsScheduledStop.lowFloorAccess,
        parkAndRidePark: journeyDetailsScheduledStop.parkAndRidePark,
        suitableForHeavilyDisabled: journeyDetailsScheduledStop.suitableForHeavilyDisabled,
        toilet: journeyDetailsScheduledStop.toilet,
        wheelChairAccessToilet: journeyDetailsScheduledStop.wheelChairAccessToilet,
        otherTransportModes: journeyDetailsScheduledStop.otherTransportModes,
    }
}

export function toJourneyDetailsWithTimes(journeyDetails: JourneyDetails): JourneyDetailsWithTimes {
    return {
        relationalId: journeyDetails.relationalId,
        routeId: journeyDetails.routeId,
        stops: journeyDetails.stops.map(toJourneyDetailsScheduledStopWithTimes),
        transportBans: journeyDetails.transportBans,
        lineVersion: journeyDetails.lineVersion,
        requiresOrdering: journeyDetails.requiresOrdering,
        baggageStorage: journeyDetails.baggageStorage,
        cyclesAllowed: journeyDetails.cyclesAllowed,
        lowFloorAccess: journeyDetails.lowFloorAccess,
        reservationCompulsory: journeyDetails.reservationCompulsory,
        reservationPossible: journeyDetails.reservationPossible,
        snacksOnBoard: journeyDetails.snacksOnBoard,
        unaccompaniedMinorAssistance: journeyDetails.unaccompaniedMinorAssistance,
    }
}
