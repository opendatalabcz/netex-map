import type { MapJourneyWithDates, MapRoute } from '@/api/model/journeysOperatingInFrame'

type PositionedMapJourneyWithDates = MapJourneyWithDates & {
    position: number[] | null | undefined
    segmentIndex: number | null | undefined
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

function getPositionFromRouteFractions(
    routeFractions: number[],
    pointSequence: number[][],
    totalRouteDistance: number,
): number[][] {
    const result: number[][] = []
    let cumulativeDistance = 0
    let previousCumulativeDistance = 0
    let pointIndex = 0
    for (const routeFraction of routeFractions) {
        if (routeFraction <= 0) {
            result.push(pointSequence[0]!)
            continue
        }
        if (routeFraction >= 1) {
            result.push(pointSequence[pointSequence.length - 1]!)
            continue
        }
        const distanceThreshold = routeFraction * totalRouteDistance
        while (cumulativeDistance < distanceThreshold) {
            previousCumulativeDistance = cumulativeDistance
            cumulativeDistance += distanceBetweenPoints(
                pointSequence[pointIndex]!,
                pointSequence[++pointIndex]!,
            )
        }
        const fromPoint = pointSequence[pointIndex - 1]!
        const toPoint = pointSequence[pointIndex]!
        const lerpFraction = inverseLerp(
            previousCumulativeDistance,
            cumulativeDistance,
            distanceThreshold,
        )
        result.push([
            lerp(fromPoint[0]!, toPoint[0]!, lerpFraction),
            lerp(fromPoint[1]!, toPoint[1]!, lerpFraction),
        ])
    }
    return result
}

function getPositionFromRouteFraction(routeFraction: number, route: MapRoute): number[] {
    return getPositionFromRouteFractions(
        [routeFraction],
        route.pointSequence.coordinates,
        route.totalDistance,
    )[0]!
}

function interpolatePositionBetweenStops(
    timeFraction: number,
    segment: number,
    route: MapRoute,
): number[] {
    const departureStopFraction = route.routeStops[segment]!
    const arrivalStopFraction = route.routeStops[segment + 1]!
    const distanceThreshold = lerp(departureStopFraction, arrivalStopFraction, timeFraction)
    return getPositionFromRouteFraction(distanceThreshold, route)
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
    journey.position = interpolatePositionBetweenStops(
        lerpFraction,
        segment,
        routeMap.get(journey.routeId)!,
    )
    journey.segmentIndex = segment
    return
}

function recalculateVehiclePosition(
    moment: Date,
    journey: PositionedMapJourneyWithDates,
    routeMap: Map<number, MapRoute>,
): void {
    if (journey.segmentIndex == null)
        journey.segmentIndex = journey.fromPreviousDay
            ? getRouteSegmentIndex(moment, journey, journey.nextDayFirstStopIndex! - 1)
            : getRouteSegmentIndex(moment, journey, 0)
    if (journey.segmentIndex != null)
        interpolateVehiclePosition(moment, journey, journey.segmentIndex, routeMap)
}

export type { PositionedMapJourneyWithDates }

export {
    recalculateVehiclePosition,
    getPositionFromRouteFraction,
    getPositionFromRouteFractions,
}
