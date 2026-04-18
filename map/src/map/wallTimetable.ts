import type { JourneyDirection } from '@/api/model/enums'
import type {
    WallJourneyPattern,
    WallJourneyWithTimes,
    WallOperatingPeriodWithDates,
    WallStop,
    WallTariffStop,
    WallTimetableWithDates,
} from '@/api/model/wallTimetable'
import {
    displayFacilitiesForCombinedStop,
    displayFacilitiesForJourney,
    type CombinedStopFacilities,
    type DisplayFacilities,
} from '@/map/facilities'
import type LocalTime from '@/util/localTime'

export type DisplayStop = {
    stopName: string
    facilities: DisplayFacilities
    banGroups: number[]
}
export type DisplayJourney = {
    relationalId: number
    routeId: number | null
    facilities: DisplayFacilities
    schedule: (LocalTime | null)[]
}
export type DisplayOperatingPeriod = WallOperatingPeriodWithDates & {
    journeys: DisplayJourney[]
    regularDays: string[]
    alsoOperatesIn: Date[]
    doesNotOperateIn: Date[]
}
export type DisplayWallTimetable = {
    direction: JourneyDirection
    text: string
    displayStops: DisplayStop[]
    displayOperatingPeriods: DisplayOperatingPeriod[]
}

export function getDisplayFacilities(
    tariffStopIdx: number,
    stop: WallStop,
    journeyPatterns: WallJourneyPattern[],
    t: (key: string) => string,
): DisplayFacilities {
    const facilitiesFromJourneyPatterns = journeyPatterns.reduce(
        (acc, journeyPattern) => {
            for (const journeyPatternStop of journeyPattern.stops) {
                if (journeyPatternStop.tariffOrder !== tariffStopIdx) continue
                if (journeyPatternStop.forBoarding) acc.forBoarding = true
                if (journeyPatternStop.forAlighting) acc.forAlighting = true
                if (journeyPatternStop.requiresOrdering) acc.requiresOrdering = true
                if (journeyPatternStop.stopOnRequest) acc.stopOnRequest = true
                break
            }
            return acc
        },
        {
            forBoarding: false,
            forAlighting: false,
            requiresOrdering: false,
            stopOnRequest: false,
        },
    )
    const combinedStop: CombinedStopFacilities = {
        forBoarding: facilitiesFromJourneyPatterns.forBoarding,
        forAlighting: facilitiesFromJourneyPatterns.forAlighting,
        requiresOrdering: facilitiesFromJourneyPatterns.requiresOrdering,
        stopOnRequest: facilitiesFromJourneyPatterns.stopOnRequest,
        bistro: stop.bistro,
        borderCrossing: stop.borderCrossing,
        displaysForVisuallyImpaired: stop.displaysForVisuallyImpaired,
        lowFloorAccess: stop.lowFloorAccess,
        parkAndRidePark: stop.parkAndRidePark,
        suitableForHeavilyDisabled: stop.suitableForHeavilyDisabled,
        toilet: stop.toilet,
        wheelChairAccessToilet: stop.wheelChairAccessToilet,
        otherTransportModes: stop.otherTransportModes,
    }
    return displayFacilitiesForCombinedStop(combinedStop, t)
}

export function getTariffStopList(
    tariffStops: WallTariffStop[],
    journeyPatterns: WallJourneyPattern[],
    stops: WallStop[],
    t: (key: string) => string,
): DisplayStop[] {
    const transportBanGroups = Array.from(
        new Map(
            journeyPatterns
                .filter(
                    (jp) =>
                        jp.transportBans != null &&
                        (jp.direction === 'OUTBOUND' || jp.direction === 'CLOCKWISE'),
                )
                .flatMap((journeyPattern) =>
                    journeyPattern.transportBans!.map((banGroup) =>
                        banGroup.map(
                            (journeyPatternStopIdx) =>
                                journeyPattern.stops[journeyPatternStopIdx]!.tariffOrder,
                        ),
                    ),
                )
                .map((banGroup) => [banGroup.join(','), banGroup]),
        ).values(),
    )
    return tariffStops.map((tariffStop, tariffStopIdx) => {
        const stop = stops.find((s) => s.relationalId === tariffStop.stopId)!
        return {
            stopName: stop.name,
            facilities: getDisplayFacilities(tariffStopIdx, stop, journeyPatterns, t),
            banGroups: transportBanGroups.reduce((acc, cur, groupIdx) => {
                if (cur.includes(tariffStopIdx)) acc.push(groupIdx)
                return acc
            }, []),
        }
    })
}

export function getDisplayJourneysForDirection(
    direction: JourneyDirection,
    journeys: WallJourneyWithTimes[],
    journeyPatterns: WallJourneyPattern[],
    tariffStopCount: number,
    t: (key: string) => string,
): DisplayJourney[] {
    const res: DisplayJourney[] = []
    for (const journey of journeys) {
        const journeyPattern = journeyPatterns.find(
            (jp) => jp.patternNumber === journey.patternNumber,
        )!
        const displayJourney: DisplayJourney = {
            relationalId: journey.relationalId,
            routeId: journeyPattern.routeId,
            facilities: displayFacilitiesForJourney(journey, t),
            schedule: [],
        }
        res.push(displayJourney)
        let journeyPatternStopIdx = 0
        const reverseOrder = direction !== 'CLOCKWISE' && direction !== 'OUTBOUND'
        let tariffStopIdx = reverseOrder ? tariffStopCount - 1 : 0
        while (reverseOrder ? tariffStopIdx >= 0 : tariffStopIdx < tariffStopCount) {
            const patternStop = journeyPattern.stops[journeyPatternStopIdx]
            if (patternStop != null && patternStop.tariffOrder === tariffStopIdx) {
                const scheduledStop = journey.schedule[journeyPatternStopIdx]!
                displayJourney.schedule.push(scheduledStop.departure ?? scheduledStop.arrival!)
                journeyPatternStopIdx += 1
            } else {
                displayJourney.schedule.push(null)
            }
            tariffStopIdx += reverseOrder ? -1 : 1
        }
    }
    res.sort((a, b) => {
        const aFirstScheduledStop = a.schedule.find((ss) => ss != null)!
        const bFirstScheduledStop = b.schedule.find((ss) => ss != null)!
        return aFirstScheduledStop.totalMillis - bFirstScheduledStop.totalMillis
    })
    return res
}

export function getDisplayOperatingPeriodsForDirection(
    direction: JourneyDirection,
    journeysByOperatingPeriod: [number, WallJourneyWithTimes[]][],
    operatingPeriods: WallOperatingPeriodWithDates[],
    journeyPatterns: WallJourneyPattern[],
    tariffStopCount: number,
    t: (key: string) => string,
): DisplayOperatingPeriod[] {
    return journeysByOperatingPeriod.map(([operatingPeriodId, journeys]) => {
        const operatingPeriod = operatingPeriods.find(
            (op) => op.relationalId === operatingPeriodId,
        )!
        const res = { ...operatingPeriod } as DisplayOperatingPeriod
        res.regularDays = Object.entries(operatingPeriod.operatingDays)
            .filter(([, active]) => active)
            .map(([day]) => day)
        res.alsoOperatesIn = []
        res.doesNotOperateIn = []
        for (const [operationExceptionType, days] of operatingPeriod.operationExceptions) {
            switch (operationExceptionType) {
                case 'ALSO_OPERATES': {
                    res.alsoOperatesIn = days
                    break
                }
                case 'DOES_NOT_OPERATE': {
                    res.doesNotOperateIn = days
                    break
                }
            }
        }
        res.journeys = getDisplayJourneysForDirection(
            direction,
            journeys,
            journeyPatterns,
            tariffStopCount,
            t,
        )
        return res
    })
}

export function getDisplayWallTimetable(
    wallTimetable: WallTimetableWithDates,
    t: (key: string) => string,
): DisplayWallTimetable[] {
    const displayStops = getTariffStopList(
        wallTimetable.lineVersion.tariffStops,
        wallTimetable.journeyPatterns,
        wallTimetable.lineVersion.stops,
        t,
    )
    const reversedDisplayStops = [...displayStops].reverse()
    return Array.from(wallTimetable.journeys.entries()).map(([direction, journeysMap]) => {
        let usedDisplayStops: DisplayStop[] = []
        switch (direction) {
            case 'CLOCKWISE':
            case 'OUTBOUND': {
                usedDisplayStops = displayStops
                break
            }
            case 'ANTICLOCKWISE':
            case 'INBOUND': {
                usedDisplayStops = reversedDisplayStops
                break
            }
        }
        return {
            direction: direction,
            text:
                t('lineVersion.inDirectionTo') +
                ' ' +
                usedDisplayStops[usedDisplayStops.length - 1]!.stopName,
            displayStops: usedDisplayStops,
            displayOperatingPeriods: getDisplayOperatingPeriodsForDirection(
                direction,
                Array.from(journeysMap.entries()),
                wallTimetable.operatingPeriods,
                wallTimetable.journeyPatterns,
                wallTimetable.lineVersion.tariffStops.length,
                t,
            ),
        }
    })
}
