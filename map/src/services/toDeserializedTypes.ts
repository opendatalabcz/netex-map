import type { EncodedRoute, Route } from '@/api/model/encodedRoute'
import type { JourneyDirection } from '@/api/model/enums'
import type {
    JourneyDetails,
    JourneyDetailsScheduledStop,
    JourneyDetailsScheduledStopWithTimes,
    JourneyDetailsWithTimes,
} from '@/api/model/journeyDetails'
import type {
    FrameJourney,
    FrameScheduledStop,
    FrameJourneyWithDates,
    FrameScheduledStopWithDates,
} from '@/api/model/journeysOperatingInFrame'
import type {
    SearchLineVersion,
    SearchLineVersionsActivePeriod,
    SearchLineVersionsActivePeriodWithDates,
    SearchLineVersionWithDates,
} from '@/api/model/searchLineVersions'
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
import Polyline from '@mapbox/polyline'

export function toMapScheduledStopWithDates(
    mapScheduledStop: FrameScheduledStop,
): FrameScheduledStopWithDates {
    return {
        arrival: mapScheduledStop.arrival == null ? null : new Date(mapScheduledStop.arrival),
        departure: mapScheduledStop.departure == null ? null : new Date(mapScheduledStop.departure),
    }
}

export function toMapJourneyWithDates(mapJourney: FrameJourney): FrameJourneyWithDates {
    return {
        relationalId: mapJourney.relationalId,
        lineVersionId: mapJourney.lineVersionId,
        routeId: mapJourney.routeId,
        schedule: mapJourney.schedule.map(toMapScheduledStopWithDates),
        nextDayFirstStopIndex: mapJourney.nextDayFirstStopIndex,
        fromPreviousDay: mapJourney.fromPreviousDay,
    }
}

export function toRoute(route: EncodedRoute): Route {
    return {
        relationalId: route.relationalId,
        pointSequence: Polyline.decode(route.pointSequence),
        totalDistance: route.totalDistance,
        routeStopFractions: route.routeStopFractions,
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
        detour: wallLineVersion.detour,
        validFrom: new Date(wallLineVersion.validFrom),
        validTo: new Date(wallLineVersion.validTo),
        operator: wallLineVersion.operator,
        activePeriods: wallLineVersion.activePeriods.map(toWallActivePeriodWithDates),
        tariffStops: wallLineVersion.tariffStops,
        stops: wallLineVersion.stops,
    }
}

export function toWallJourneyWithDates(wallJourney: WallJourney): WallJourneyWithTimes {
    return {
        relationalId: wallJourney.relationalId,
        patternNumber: wallJourney.patternNumber,
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
    const operationExceptions = new Map()
    for (const [key, value] of Object.entries(wallOperatingPeriod.operationExceptions)) {
        operationExceptions.set(
            key,
            value.map((day) => new Date(day)),
        )
    }
    return {
        relationalId: wallOperatingPeriod.relationalId,
        operatingDays: wallOperatingPeriod.operatingDays,
        operationExceptions: operationExceptions,
    }
}

export function toWallTimetableWithDates(wallTimetable: WallTimetable): WallTimetableWithDates {
    const journeys: Map<JourneyDirection, Map<number, WallJourneyWithTimes[]>> = new Map()
    for (const [direction, journeyMap] of Object.entries(wallTimetable.journeys)) {
        const journeysWithTimesMap: Map<number, WallJourneyWithTimes[]> = new Map()
        for (const [id, journeys] of Object.entries(journeyMap)) {
            journeysWithTimesMap.set(Number(id), journeys.map(toWallJourneyWithDates))
        }
        journeys.set(direction as JourneyDirection, journeysWithTimesMap)
    }
    return {
        lineVersion: toWallLineVersionWithDates(wallTimetable.lineVersion),
        operatingPeriods: wallTimetable.operatingPeriods.map(toWallOperatingPeriodWithDates),
        journeyPatterns: wallTimetable.journeyPatterns,
        journeys: journeys,
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

export function toSearchLineVersionsActivePeriodWithDates(
    searchLineVersionsActivePeriod: SearchLineVersionsActivePeriod,
): SearchLineVersionsActivePeriodWithDates {
    return {
        fromDate: new Date(searchLineVersionsActivePeriod.fromDate),
        toDate: new Date(searchLineVersionsActivePeriod.toDate),
    }
}

export function toSearchLineVersionWithDates(
    searchLineVersion: SearchLineVersion,
): SearchLineVersionWithDates {
    return {
        relationalId: searchLineVersion.relationalId,
        publicCode: searchLineVersion.publicCode,
        name: searchLineVersion.name,
        shortName: searchLineVersion.shortName,
        transportMode: searchLineVersion.transportMode,
        lineType: searchLineVersion.lineType,
        detour: searchLineVersion.detour,
        validFrom: new Date(searchLineVersion.validFrom),
        validTo: new Date(searchLineVersion.validTo),
        operator: searchLineVersion.operator,
        activePeriods: searchLineVersion.activePeriods.map(
            toSearchLineVersionsActivePeriodWithDates,
        ),
    }
}
