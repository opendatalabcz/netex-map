import type { Journey, JourneyWithDatesAndTimes } from "@/api/model/journey";
import type { Route } from "@/api/model/route";
import LocalTime from "@/util/localTime";

/**
 * This function assumes that it **will** find the `moment` in the `schedule` within specified bounds.
 * Also this function doesn't handle day transitions.
 */
function getRouteSegmentIndexTraversingSchedule(journey: Journey, moment: LocalTime, from: number, to: number): number {
    for (let i = from; i < to; i++) {
        const curStop = journey.schedule[i]!
        const departureTime = LocalTime.parse(curStop.departure ?? curStop.arrival!)
        if (moment.isBefore(departureTime)) {
            return i - 1
        }
    }
    return to - 2 // Assume that departure time of last stop == moment
}

/**
 * This function doesn't handle following edge cases:
 * 
 * 1. First stop starts at the previous day and ends this day; and the `moment` is at this stop
 * 2. Last stop starts at this day and ends the next day; and the `moment` is at this stop
 */
function getRouteSegmentIndexOfJourneyFromPreviousDay(journey: Journey, moment: LocalTime): number | null {
    if (journey.schedule.length < 2) {
        console.warn("Calculating position of vehicle of journey that doesn't have at least 2 stops.")
        return null
    }
    if (journey.nextDayFirstStopIndex == null) {
        console.warn("Calculating position of vehicle of journey from previous day, but the journey doesn't continue to this day.")
        return null
    }
    const lastStop = journey.schedule[journey.schedule.length - 1]!
    const lastTime = LocalTime.parse(lastStop.departure ?? lastStop.arrival!)
    if (moment.isAfter(lastTime)) {
        return null
    }
    const firstStopThisDay = journey.schedule[journey.nextDayFirstStopIndex]!
    const firstTimeThisDay = LocalTime.parse(firstStopThisDay.arrival ?? firstStopThisDay.departure!)
    if (moment.isBefore(firstTimeThisDay)) {
        return journey.nextDayFirstStopIndex - 1
    }
    return getRouteSegmentIndexTraversingSchedule(journey, moment, journey.nextDayFirstStopIndex, journey.schedule.length)
}

/**
 * This function doesn't handle following edge cases:
 * 
 * 1. First stop starts at the previous day and ends this day; and the `moment` is at this stop
 * 2. Last stop starts at this day and ends the next day; and the `moment` is at this stop
 */
function getRouteSegmentIndex(journey: Journey, moment: LocalTime): number | null {
    if (journey.schedule.length < 2) {
        console.warn("Calculating position of vehicle of journey that doesn't have at least 2 stops.")
        return null
    }
    const firstStop = journey.schedule[0]!
    const firstTime = LocalTime.parse(firstStop.arrival ?? firstStop.departure!)
    if (moment.isBefore(firstTime)) {
        return null
    }
    if (journey.nextDayFirstStopIndex == null) {
        const lastStop = journey.schedule[journey.schedule.length - 1]!
        const lastTime = LocalTime.parse(lastStop.departure ?? lastStop.arrival!)
        if (moment.isAfter(lastTime)) return null
        return getRouteSegmentIndexTraversingSchedule(journey, moment, 0, journey.schedule.length)
    }
    
    const lastStopThisDay = journey.schedule[journey.nextDayFirstStopIndex - 1]!
    const lastStopThisDayArrival = lastStopThisDay.arrival == null ? null : LocalTime.parse(lastStopThisDay.arrival)
    const lastStopThisDayDeparture = lastStopThisDay.departure == null ? null : LocalTime.parse(lastStopThisDay.departure)
    if (moment.isBefore(lastStopThisDayArrival ?? lastStopThisDayDeparture!)) {
        return getRouteSegmentIndexTraversingSchedule(journey, moment, 0, journey.nextDayFirstStopIndex)
    }
    return journey.nextDayFirstStopIndex - 1
}

function linearInterpolation(a: number, b: number, t: number): number {
    return a + (b - a) * t
}

function positionForStop(journey: Journey | JourneyWithDatesAndTimes, stopIndex: number): number[] {
    const pointSequenceIndex = journey.route!.routeStops[stopIndex]!.pointSequenceIndex
    return journey.route!.pointSequence[pointSequenceIndex]!
}

type VehiclePositionResult = {
    position: number[],
    segment: number,
}

function lerpPosition(
    from: LocalTime,
    to: LocalTime,
    moment: LocalTime,
    journey: JourneyWithDatesAndTimes,
    routeSegmentIndex: number,
): VehiclePositionResult {
    const fromPosition = positionForStop(journey, routeSegmentIndex)
    const toPosition = positionForStop(journey, routeSegmentIndex + 1)
    const fraction = (moment.totalMillis - from.totalMillis) / (to.totalMillis - from.totalMillis)
    const latLerp = linearInterpolation(fromPosition[0]!, toPosition[0]!, fraction)
    const lonLerp = linearInterpolation(fromPosition[1]!, toPosition[1]!, fraction)
    return {
        position: [latLerp, lonLerp],
        segment: routeSegmentIndex,
    }
}

function calculateVehiclePositionBeforeDepartureTime(
    journey: JourneyWithDatesAndTimes,
    moment: LocalTime,
    routeSegmentIndex: number,
): VehiclePositionResult | null {
    const departureStop = journey.schedule[routeSegmentIndex]!
    if ((departureStop.arrival != null && departureStop.departure != null)
        && (moment.isAfter(departureStop.arrival) || departureStop.arrival.isAfter(departureStop.departure))
    ) {
        return {
            position: positionForStop(journey, routeSegmentIndex),
            segment: routeSegmentIndex
        }
    }
    const hasPrevSection = routeSegmentIndex > 0
    return hasPrevSection ? calculateVehiclePosition(journey, moment, routeSegmentIndex - 1) : null
}

function calculateVehiclePositionAfterArrivalTime(
    journey: JourneyWithDatesAndTimes,
    moment: LocalTime,
    routeSegmentIndex: number,
): VehiclePositionResult | null {
    const arrivalStop = journey.schedule[routeSegmentIndex + 1]!
    if ((arrivalStop.arrival != null && arrivalStop.departure != null)
         && (moment.isBefore(arrivalStop.departure) || arrivalStop.arrival.isAfter(arrivalStop.departure))
    ) {
        return {
            position: positionForStop(journey, routeSegmentIndex + 1),
            segment: routeSegmentIndex,
        }
    }
    const hasNextSection = routeSegmentIndex < journey.schedule.length - 2
    return hasNextSection ? calculateVehiclePosition(journey, moment, routeSegmentIndex + 1) : null
}

function calculateVehiclePositionWhenDayOverlap(
    journey: JourneyWithDatesAndTimes,
    moment: LocalTime,
    routeSegmentIndex: number,
    departureTime: LocalTime,
    arrivalTime: LocalTime,
): VehiclePositionResult | null {
    if (!moment.isBefore(departureTime)) {
        return lerpPosition(departureTime, LocalTime.LAST_MOMENT_IN_DAY, moment, journey, routeSegmentIndex)
    }
    if (!moment.isAfter(arrivalTime)) {
        return lerpPosition(LocalTime.FIRST_MOMENT_IN_DAY, arrivalTime, moment, journey, routeSegmentIndex)
    }
    const arrivalStop = journey.schedule[routeSegmentIndex + 1]!
    if (moment.isAfter(arrivalTime)) {
        if (arrivalStop.arrival != null && arrivalStop.departure != null && moment.isBefore(arrivalStop.departure)) {
            return {
                position: positionForStop(journey, routeSegmentIndex + 1),
                segment: routeSegmentIndex,
            }
        }
        const hasNextSection = routeSegmentIndex < journey.schedule.length - 2
        return hasNextSection ? calculateVehiclePosition(journey, moment, routeSegmentIndex + 1) : null
    }
    const departureStop = journey.schedule[routeSegmentIndex]!
    if (departureStop.arrival != null && departureStop.departure != null && moment.isAfter(departureStop.arrival)) {
        return {
            position: positionForStop(journey, routeSegmentIndex),
            segment: routeSegmentIndex
        }
    }
    const hasPrevSection = routeSegmentIndex > 0
    return hasPrevSection ? calculateVehiclePosition(journey, moment, routeSegmentIndex - 1) : null
}

function calculateVehiclePosition(journey: JourneyWithDatesAndTimes, moment: LocalTime, routeSegmentIndex: number): VehiclePositionResult | null {
    const departureStop = journey.schedule[routeSegmentIndex]!
    const arrivalStop = journey.schedule[routeSegmentIndex + 1]!
    const departureTime = departureStop.departure ?? departureStop.arrival!
    const arrivalTime = arrivalStop.arrival ?? arrivalStop.departure!
    
    if (departureTime.isAfter(arrivalTime)) {
        return calculateVehiclePositionWhenDayOverlap(journey, moment, routeSegmentIndex, departureTime, arrivalTime)
    }
    if (moment.isAfter(arrivalTime)) {
        return calculateVehiclePositionAfterArrivalTime(journey, moment, routeSegmentIndex)
    }
    if (moment.isBefore(departureTime)) {
        return calculateVehiclePositionBeforeDepartureTime(journey, moment, routeSegmentIndex)
    }
    return lerpPosition(departureTime, arrivalTime, moment, journey, routeSegmentIndex)
}

