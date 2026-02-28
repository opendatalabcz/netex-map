import type { Journey, JourneyWithDatesAndTimes } from "@/api/model/journey"
import type { LineVersion, LineVersionWithDates } from "@/api/model/lineVersion"
import type { OperatingPeriod, OperatingPeriodWithDates } from "@/api/model/operatingPeriod"
import type { ScheduledStop, ScheduledStopWithTimes } from "@/api/model/scheduledStop"
import LocalTime from "@/util/localTime"

function toLineVersionWithDates(lineVersion: LineVersion): LineVersionWithDates {
    return {
        lineId: lineVersion.lineId,
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
        departure: scheduledStop.departure == null ? null : LocalTime.parse(scheduledStop.departure),
    }
}

function toJourneyWithDatesAndTimes(journey: Journey): JourneyWithDatesAndTimes {
    return {
        journeyId: journey.journeyId,
        lineVersion: toLineVersionWithDates(journey.lineVersion),
        journeyPatternId: journey.journeyPatternId,
        schedule: journey.schedule.map(toScheduledStopWithTimes),
        operatingPeriods: journey.operatingPeriods.map(toOperatingPeriodWithDates),
        route: journey.route,
        nextDayFirstStopIndex: journey.nextDayFirstStopIndex,
    }
}

export {
    toLineVersionWithDates,
    toOperatingPeriodWithDates,
    toScheduledStopWithTimes,
    toJourneyWithDatesAndTimes,
}
