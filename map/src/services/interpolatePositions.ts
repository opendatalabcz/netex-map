import type { MapJourneyWithDates, MapRoute } from '@/api/model/journeysOperatingInFrame'

type PositionedMapJourneyWithDates = MapJourneyWithDates & {
    position: number[] | null | undefined
    segmentIndex: number | null | undefined
    azimuth: number | null | undefined
    routePointIndex: number | null
    routeCumulativeDistance: number | null
}

type InterpolationData = {
    position: number[]
    azimuth?: number | undefined
    pointIndexHint: number
    cumulativeDistanceHint: number
}

const EARTH_RADIUS = 6_371_000
const LAT = 0
const LON = 1

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
    const cosLat = Math.cos(a[LAT]! * Math.PI / 180)
    const dLonRad = (b[LON]! - a[LON]!) * Math.PI / 180 * cosLat
    const dLatRad = (b[LAT]! - a[LAT]!) * Math.PI / 180
    return EARTH_RADIUS * Math.sqrt(dLatRad * dLatRad + dLonRad * dLonRad)
}

function lerp(a: number, b: number, f: number): number {
    return a + (b - a) * f
}

function inverseLerp(a: number, b: number, c: number): number {
    return (c - a) / (b - a)
}

function calculateAzimuth(a: number[], b: number[]): number {
    const dLon = b[LON]! - a[LON]!
    const dLat = b[LAT]! - a[LAT]!
    const cosLatRad = Math.cos(a[LAT]! * Math.PI / 180)
    const angle = Math.atan2(dLon * cosLatRad, dLat) * (180 / Math.PI)
    return (angle + 360) % 360
}

function getInterpolationDataFromRouteFractions(
    routeFractions: number[],
    pointSequence: [number, number][],
    totalRouteDistance: number,
    includeAzimuth: boolean = false,
    pointIndexHint: number = 0,
    cumulativeDistanceHint: number = 0,
): InterpolationData[] {
    const result: InterpolationData[] = []
    let cumulativeDistance = cumulativeDistanceHint
    let previousCumulativeDistance = cumulativeDistance
    let pointIndex = pointIndexHint
    for (const routeFraction of routeFractions) {
        if (routeFraction <= 0) {
            const first = pointSequence[0]!
            const second = pointSequence[1]!
            result.push({
                position: first,
                azimuth: includeAzimuth ? calculateAzimuth(first, second) : undefined,
                pointIndexHint: pointIndex,
                cumulativeDistanceHint: cumulativeDistance,
            })
            continue
        }
        if (routeFraction >= 1) {
            const last = pointSequence[pointSequence.length - 1]!
            const secondLast = pointSequence[pointSequence.length - 2]!
            result.push({
                position: last,
                azimuth: includeAzimuth ? calculateAzimuth(secondLast, last) : undefined,
                pointIndexHint: pointIndex,
                cumulativeDistanceHint: cumulativeDistance,
            })
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
        result.push({
            position: [
                lerp(fromPoint[LAT]!, toPoint[LAT]!, lerpFraction),
                lerp(fromPoint[LON]!, toPoint[LON]!, lerpFraction),
            ],
            azimuth: includeAzimuth ? calculateAzimuth(fromPoint, toPoint) : undefined,
            pointIndexHint: pointIndex - 1,
            cumulativeDistanceHint: previousCumulativeDistance,
        })
    }
    return result
}

function getInterpolationDataFromRouteFraction(
    routeFraction: number,
    route: MapRoute,
    pointIndexHint: number | null,
    cumulativeDistanceHint: number | null,
): InterpolationData {
    return getInterpolationDataFromRouteFractions(
        [routeFraction],
        route.pointSequence,
        route.totalDistance,
        true,
        pointIndexHint ?? 0,
        cumulativeDistanceHint ?? 0,
    )[0]!
}

function interpolatePositionBetweenStops(
    timeFraction: number,
    segment: number,
    route: MapRoute,
    pointIndexHint: number | null,
    cumulativeDistanceHint: number | null,
): InterpolationData {
    const departureStopFraction = route.routeStops[segment]!
    const arrivalStopFraction = route.routeStops[segment + 1]!
    const distanceThreshold = lerp(departureStopFraction, arrivalStopFraction, timeFraction)
    return getInterpolationDataFromRouteFraction(distanceThreshold, route, pointIndexHint, cumulativeDistanceHint)
}

function interpolateVehiclePosition(
    moment: Date,
    journey: PositionedMapJourneyWithDates,
    segment: number,
    routeMap: MapRoute,
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
    const interpolationData = interpolatePositionBetweenStops(lerpFraction, segment, routeMap, journey.routePointIndex, journey.routeCumulativeDistance)
    journey.position = interpolationData.position
    journey.azimuth = interpolationData.azimuth
    journey.segmentIndex = segment
    journey.routePointIndex = interpolationData.pointIndexHint
    journey.routeCumulativeDistance = interpolationData.cumulativeDistanceHint
    return
}

function recalculateVehiclePosition(
    moment: Date,
    journey: PositionedMapJourneyWithDates,
    routeMap: MapRoute,
): void {
    if (journey.segmentIndex == null)
        journey.segmentIndex = journey.fromPreviousDay
            ? getRouteSegmentIndex(moment, journey, journey.nextDayFirstStopIndex! - 1)
            : getRouteSegmentIndex(moment, journey, 0)
    if (journey.segmentIndex != null)
        interpolateVehiclePosition(moment, journey, journey.segmentIndex, routeMap)
}

export type { PositionedMapJourneyWithDates, InterpolationData }

export {
    recalculateVehiclePosition,
    getInterpolationDataFromRouteFraction,
    getInterpolationDataFromRouteFractions,
}
