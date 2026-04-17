import type { FrameJourneyWithDates } from '@/api/model/journeysOperatingInFrame'
import type { Route } from '@/api/model/encodedRoute'

type PositionedMapJourneyWithDates = FrameJourneyWithDates & {
    position: number[] | null | undefined
    segmentIndex: number | null | undefined
    azimuth: number | null | undefined
}

type InterpolationData = {
    position: number[]
    azimuth?: number | undefined
}

const EARTH_RADIUS = 6_371_000
const LAT = 0
const LON = 1

function getRouteSegmentIndex(
    moment: Date,
    journey: FrameJourneyWithDates,
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
    const cosLat = Math.cos((a[LAT]! * Math.PI) / 180)
    const dLonRad = (((b[LON]! - a[LON]!) * Math.PI) / 180) * cosLat
    const dLatRad = ((b[LAT]! - a[LAT]!) * Math.PI) / 180
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
    const cosLatRad = Math.cos((a[LAT]! * Math.PI) / 180)
    const angle = Math.atan2(dLon * cosLatRad, dLat) * (180 / Math.PI)
    return (angle + 360) % 360
}

function getInterpolationDataFromRouteFractions(
    routeFractions: number[],
    pointSequence: [number, number][],
    totalRouteDistance: number,
    includeAzimuth: boolean = false,
): InterpolationData[] {
    const result: InterpolationData[] = []
    let cumulativeDistance = 0
    let previousCumulativeDistance = 0
    let pointIndex = 0
    for (const routeFraction of routeFractions) {
        if (routeFraction <= 0) {
            const first = pointSequence[0]!
            const second = pointSequence[1]!
            result.push({
                position: first,
                azimuth: includeAzimuth ? calculateAzimuth(first, second) : undefined,
            })
            continue
        }
        if (routeFraction >= 1) {
            const last = pointSequence[pointSequence.length - 1]!
            const secondLast = pointSequence[pointSequence.length - 2]!
            result.push({
                position: last,
                azimuth: includeAzimuth ? calculateAzimuth(secondLast, last) : undefined,
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
        })
    }
    return result
}

function getInterpolationDataFromRouteFraction(
    routeFraction: number,
    route: Route,
): InterpolationData {
    return getInterpolationDataFromRouteFractions(
        [routeFraction],
        route.pointSequence,
        route.totalDistance,
        true,
    )[0]!
}

function interpolatePositionBetweenStops(
    timeFraction: number,
    segment: number,
    route: Route,
): InterpolationData {
    const departureStopFraction = route.routeStopFractions[segment]!
    const arrivalStopFraction = route.routeStopFractions[segment + 1]!
    const distanceThreshold = lerp(departureStopFraction, arrivalStopFraction, timeFraction)
    return getInterpolationDataFromRouteFraction(distanceThreshold, route)
}

function interpolateVehiclePosition(
    moment: Date,
    journey: PositionedMapJourneyWithDates,
    segment: number,
    route: Route,
): void {
    if (journey.routeId == null) {
        journey.position = undefined
        journey.segmentIndex = undefined
        journey.azimuth = undefined
        return
    }

    const momentTime = moment.getTime()
    const arrivalStop = journey.schedule[segment + 1]!
    const arrivalTime = (arrivalStop.arrival ?? arrivalStop.departure!).getTime()
    if (momentTime >= arrivalTime) {
        if (segment < journey.schedule.length - 2) {
            return interpolateVehiclePosition(moment, journey, segment + 1, route)
        }
        journey.position = null
        journey.segmentIndex = null
        journey.azimuth = null
        return
    }
    const departureStop = journey.schedule[segment]!
    const departureTime = (departureStop.arrival ?? departureStop.departure!).getTime()
    if (momentTime < departureTime) {
        if (segment > 0) {
            return interpolateVehiclePosition(moment, journey, segment - 1, route)
        }
        journey.position = null
        journey.segmentIndex = null
        journey.azimuth = null
        return
    }

    const lerpFraction = Math.max(0, (momentTime - departureTime) / (arrivalTime - departureTime))
    const interpolationData = interpolatePositionBetweenStops(lerpFraction, segment, route)
    journey.position = interpolationData.position
    journey.azimuth = interpolationData.azimuth
    journey.segmentIndex = segment
    return
}

function recalculateVehiclePosition(
    moment: Date,
    journey: PositionedMapJourneyWithDates,
    route: Route,
): void {
    if (journey.segmentIndex == null)
        journey.segmentIndex = journey.fromPreviousDay
            ? getRouteSegmentIndex(moment, journey, journey.nextDayFirstStopIndex! - 1)
            : getRouteSegmentIndex(moment, journey, 0)
    if (journey.segmentIndex != null)
        interpolateVehiclePosition(moment, journey, journey.segmentIndex, route)
}

export type { PositionedMapJourneyWithDates, InterpolationData }

export {
    recalculateVehiclePosition,
    getInterpolationDataFromRouteFraction,
    getInterpolationDataFromRouteFractions,
}
