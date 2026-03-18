import type { MapJourneyWithDates, MapRoute } from '@/api/model/journeysOperatingInFrame'

type PositionedMapJourneyWithDates = MapJourneyWithDates & {
    position: number[] | null | undefined
    segmentIndex: number | null | undefined
}

type PositionedJourneys = {
    startingJourneys: PositionedMapJourneyWithDates[]
    continuingJourneys: PositionedMapJourneyWithDates[]
}

function getRouteSegmentIndex(
    moment: Date,
    journey: MapJourneyWithDates,
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

function stopPosition(route: MapRoute, stopIndex: number): number[] {
    return route.pointSequence.coordinates[
        route.routeStops[stopIndex]!.pointSequenceIndex
    ]!
}

function distanceBetweenPoints(a: number[], b: number[]): number {
    const dLat = b[0]! - a[0]!
    const dLon = b[1]! - a[1]!
    return Math.sqrt(dLat * dLat + dLon * dLon)
}

function lerp(a: number, b: number, f: number): number {
    return a + (b - a) * f
}

function inverseLerp(a: number, b: number, c: number): number {
    return (c - a) / (b - a)
}

function interpolatePositionBetweenStops(fraction: number, segment: number, route: MapRoute): number[] {
    if (fraction <= 0) return stopPosition(route, segment)
    if (fraction >= 1) return stopPosition(route, segment + 1)
    const departureRouteStop = route.routeStops[segment]!
    const distanceThreshold = fraction * departureRouteStop.distanceToNextStop
    let cumulativeDistance = 0
    let previousCumulativeDistance = 0
    let pointIndex = departureRouteStop.pointSequenceIndex
    while (cumulativeDistance < distanceThreshold) {
        previousCumulativeDistance = cumulativeDistance
        cumulativeDistance += distanceBetweenPoints(
            route.pointSequence.coordinates[pointIndex]!,
            route.pointSequence.coordinates[++pointIndex]!
        )
    }
    const fromPoint = route.pointSequence.coordinates[pointIndex - 1]!
    const toPoint = route.pointSequence.coordinates[pointIndex]!
    const lerpFraction = inverseLerp(previousCumulativeDistance, cumulativeDistance, distanceThreshold)
    return [
        lerp(toPoint[0]!, fromPoint[0]!, lerpFraction),
        lerp(toPoint[1]!, fromPoint[1]!, lerpFraction),
    ]
}

function interpolateVehiclePosition(
    moment: Date,
    journey: PositionedMapJourneyWithDates,
    segment: number,
    routeMap: Map<number, MapRoute>,
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
    journey.position = interpolatePositionBetweenStops(lerpFraction, segment, routeMap.get(journey.routeId)!)
    journey.segmentIndex = segment
    return
}

function calculateVehiclePositions(
    moment: Date,
    startingJourneys: MapJourneyWithDates[],
    continuingJourneys: MapJourneyWithDates[],
    routeMap: Map<number, MapRoute>,
): PositionedJourneys {
    return {
        startingJourneys: startingJourneys.map((journey) => {
            const segmentIndex = getRouteSegmentIndex(moment, journey, 0)
            const positionedJourney = journey as PositionedMapJourneyWithDates
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
            const positionedJourney = journey as PositionedMapJourneyWithDates
            if (segmentIndex != null)
                interpolateVehiclePosition(moment, positionedJourney, segmentIndex, routeMap)
            return positionedJourney
        }),
    }
}

function recalculateVehiclePositions(
    moment: Date,
    journeys: PositionedJourneys,
    routeMap: Map<number, MapRoute>,
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

export type { PositionedMapJourneyWithDates, PositionedJourneys }

export { calculateVehiclePositions, recalculateVehiclePositions }
