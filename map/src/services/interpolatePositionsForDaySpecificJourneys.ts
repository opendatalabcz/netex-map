import type { DaySpecificJourneyWithDates } from '@/api/model/journeysOperatingInDay'
import type { Route } from '@/api/model/route'

type PositionedDaySpecificJourneyWithDates = DaySpecificJourneyWithDates & {
    position: number[] | null | undefined
    segmentIndex: number | null | undefined
}

type PositionedJourneys = {
    startingJourneys: PositionedDaySpecificJourneyWithDates[]
    continuingJourneys: PositionedDaySpecificJourneyWithDates[]
}

function getRouteSegmentIndex(
    moment: Date,
    journey: DaySpecificJourneyWithDates,
    from: number,
): number | null {
    let i = Math.max(0, from)
    const firstStop = journey.schedule[i]!
    const lastStop = journey.schedule[journey.schedule.length - 1]!
    if (
        moment.getTime() < (firstStop.arrival ?? firstStop.departure!).getTime() ||
        moment.getTime() >= (lastStop.departure ?? lastStop.arrival!).getTime()
    ) {
        return null
    }

    for (; i < journey.schedule.length; i++) {
        const curStop = journey.schedule[i]!
        const departureTime = curStop.departure ?? curStop.arrival!
        if (moment.getTime() < departureTime.getTime()) {
            return i - 1
        }
    }
    return null
}

function stopPosition(routeId: number, stopIndex: number, routeMap: Map<number, Route>): number[] {
    const route = routeMap.get(routeId)!
    return route.routeStops[stopIndex]!.physicalStop.position
}

function interpolateVehiclePosition(
    moment: Date,
    journey: PositionedDaySpecificJourneyWithDates,
    segment: number,
    routeMap: Map<number, Route>,
): void {
    if (journey.routeId == null) {
        journey.position = undefined
        journey.segmentIndex = undefined
        return
    }

    const momentTime = moment.getTime()
    const arrivalStop = journey.schedule[segment + 1]!
    const arrivalTime = (arrivalStop.arrival ?? arrivalStop.departure!).getTime()
    if (momentTime >= arrivalTime) {
        if (segment < journey.schedule.length - 2) {
            return interpolateVehiclePosition(moment, journey, segment + 1, routeMap)
        }
        journey.position = null
        journey.segmentIndex = null
        return
    }
    const departureStop = journey.schedule[segment]!
    const departureTime = (departureStop.arrival ?? departureStop.departure!).getTime()
    if (momentTime < departureTime) {
        if (segment > 0) {
            return interpolateVehiclePosition(moment, journey, segment - 1, routeMap)
        }
    }

    const lerpFraction = Math.max(0, (momentTime - departureTime) / (arrivalTime - departureTime))
    const departurePosition = stopPosition(journey.routeId, segment, routeMap)
    const arrivalPosition = stopPosition(journey.routeId, segment + 1, routeMap)
    journey.position = [
        departurePosition[0]! + (arrivalPosition[0]! - departurePosition[0]!) * lerpFraction,
        departurePosition[1]! + (arrivalPosition[1]! - departurePosition[1]!) * lerpFraction,
    ]
    journey.segmentIndex = segment
    return
}

function calculateVehiclePositions(
    moment: Date,
    startingJourneys: DaySpecificJourneyWithDates[],
    continuingJourneys: DaySpecificJourneyWithDates[],
    routeMap: Map<number, Route>,
): PositionedJourneys {
    return {
        startingJourneys: startingJourneys.map((journey) => {
            const segmentIndex = getRouteSegmentIndex(moment, journey, 0)
            const positionedJourney = journey as PositionedDaySpecificJourneyWithDates
            if (segmentIndex != null)
                interpolateVehiclePosition(moment, positionedJourney, segmentIndex, routeMap)
            return positionedJourney
        }),
        continuingJourneys: continuingJourneys.map((journey) => {
            const segmentIndex = getRouteSegmentIndex(
                moment,
                journey,
                journey.nextDayFirstStopIndex! - 1,
            )
            const positionedJourney = journey as PositionedDaySpecificJourneyWithDates
            if (segmentIndex != null)
                interpolateVehiclePosition(moment, positionedJourney, segmentIndex, routeMap)
            return positionedJourney
        }),
    }
}

function recalculateVehiclePositions(
    moment: Date,
    journeys: PositionedJourneys,
    routeMap: Map<number, Route>,
): void {
    journeys.startingJourneys.forEach((journey) => {
        if (journey.segmentIndex == null)
            journey.segmentIndex = getRouteSegmentIndex(moment, journey, 0)
        if (journey.segmentIndex != null)
            interpolateVehiclePosition(moment, journey, journey.segmentIndex, routeMap)
    })
    journeys.continuingJourneys.forEach((journey) => {
        if (journey.segmentIndex == null)
            journey.segmentIndex = getRouteSegmentIndex(
                moment,
                journey,
                journey.nextDayFirstStopIndex! - 1,
            )
        if (journey.segmentIndex != null)
            interpolateVehiclePosition(moment, journey, journey.segmentIndex, routeMap)
    })
}

export type { PositionedDaySpecificJourneyWithDates, PositionedJourneys }

export { calculateVehiclePositions, recalculateVehiclePositions }
