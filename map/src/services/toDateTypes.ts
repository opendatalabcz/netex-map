import type { Journey, JourneyWithDatesAndTimes } from '@/api/model/journey'
import type { LineVersion, LineVersionWithDates } from '@/api/model/lineVersion'
import type { OperatingPeriod, OperatingPeriodWithDates } from '@/api/model/operatingPeriod'
import type { ScheduledStop, ScheduledStopWithTimes } from '@/api/model/scheduledStop'
import type {
    JourneysOperatingInDay,
    DaySpecificJourney,
    DaySpecificScheduledStop,
    JourneysOperatingInDayWithDates,
    DaySpecificJourneyWithDates,
    DaySpecificScheduledStopWithDates,
} from '@/api/model/journeysOperatingInDay'
import LocalTime from '@/util/localTime'

function toLineVersionWithDates(lineVersion: LineVersion): LineVersionWithDates {
    return {
        relationalId: lineVersion.relationalId,
        externalId: lineVersion.externalId,
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
        departure:
            scheduledStop.departure == null ? null : LocalTime.parse(scheduledStop.departure),
    }
}

function toJourneyWithDatesAndTimes(journey: Journey): JourneyWithDatesAndTimes {
    return {
        relationalId: journey.relationalId,
        externalId: journey.externalId,
        lineVersion: toLineVersionWithDates(journey.lineVersion),
        journeyPatternId: journey.journeyPatternId,
        schedule: journey.schedule.map(toScheduledStopWithTimes),
        operatingPeriods: journey.operatingPeriods.map(toOperatingPeriodWithDates),
        route: journey.route,
        nextDayFirstStopIndex: journey.nextDayFirstStopIndex,
    }
}

function toDaySpecificScheduledStopWithDates(
    daySpecificScheduledStop: DaySpecificScheduledStop,
): DaySpecificScheduledStopWithDates {
    return {
        name: daySpecificScheduledStop.name,
        stopOnRequest: daySpecificScheduledStop.stopOnRequest,
        arrival:
            daySpecificScheduledStop.arrival == null
                ? null
                : new Date(daySpecificScheduledStop.arrival),
        departure:
            daySpecificScheduledStop.departure == null
                ? null
                : new Date(daySpecificScheduledStop.departure),
    }
}

function toDaySpecificJourneyWithDates(
    daySpecificJourney: DaySpecificJourney,
): DaySpecificJourneyWithDates {
    return {
        relationalId: daySpecificJourney.relationalId,
        lineVersion: toLineVersionWithDates(daySpecificJourney.lineVersion),
        routeId: daySpecificJourney.routeId,
        schedule: daySpecificJourney.schedule.map(toDaySpecificScheduledStopWithDates),
        nextDayFirstStopIndex: daySpecificJourney.nextDayFirstStopIndex,
    }
}

function toJourneysOperatingInDayWithDates(
    journeysOperatingInDay: JourneysOperatingInDay,
): JourneysOperatingInDayWithDates {
    return {
        startingThisDay: journeysOperatingInDay.startingThisDay.map(toDaySpecificJourneyWithDates),
        continuingThisDay: journeysOperatingInDay.continuingThisDay.map(
            toDaySpecificJourneyWithDates,
        ),
        routes: journeysOperatingInDay.routes,
    }
}

export {
    toLineVersionWithDates,
    toOperatingPeriodWithDates,
    toScheduledStopWithTimes,
    toJourneyWithDatesAndTimes,
    toDaySpecificScheduledStopWithDates,
    toDaySpecificJourneyWithDates,
    toJourneysOperatingInDayWithDates,
}
