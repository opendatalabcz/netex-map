import type LocalTime from "@/util/localTime"

type WallScheduledStop = {
    name: string,
    stopOnRequest: boolean,
    arrival: string | null,
    departure: string | null,
}

type WallOperatingDays = {
    monday: boolean,
    tuesday: boolean,
    wednesday: boolean,
    thursday: boolean,
    friday: boolean,
    saturday: boolean,
    sunday: boolean,
}

type WallOperationExceptionType = 'ALSO_OPERATES' | 'DOES_NOT_OPERATE'

type WallActivePeriod = {
    fromDate: string
    toDate: string
}

type WallLineVersion = {
    relationalId: number,
    publicCode: string,
    name: string,
    shortName: string,
    transportMode: string,
    isDetour: boolean,
    activePeriods: WallActivePeriod[],
}

type WallOperatingPeriod = {
    operatingDays: WallOperatingDays,
    operationExceptions: Record<WallOperationExceptionType, string[]>,
    journeys: Record<string, WallScheduledStop[]>,
}

type WallTimetable = {
    lineVersion: WallLineVersion,
    operatingPeriods: WallOperatingPeriod[],
}

type WallScheduledStopWithTimes = {
    name: string,
    stopOnRequest: boolean,
    arrival: LocalTime | null,
    departure: LocalTime | null,
}

type WallActivePeriodWithDates = {
    fromDate: Date
    toDate: Date
}

type WallLineVersionWithDates = {
    relationalId: number,
    publicCode: string,
    name: string,
    shortName: string,
    transportMode: string,
    isDetour: boolean,
    activePeriods: WallActivePeriodWithDates[],
}

type WallOperatingPeriodWithDates = {
    operatingDays: WallOperatingDays,
    operationExceptions: Map<WallOperationExceptionType, Date[]>,
    journeys: Map<number, WallScheduledStopWithTimes[]>,
}

type WallTimetableWithDates = {
    lineVersion: WallLineVersionWithDates,
    operatingPeriods: WallOperatingPeriodWithDates[],
}

export type {
    WallScheduledStop,
    WallOperatingDays,
    WallOperationExceptionType,
    WallActivePeriod,
    WallLineVersion,
    WallOperatingPeriod,
    WallTimetable,
    WallScheduledStopWithTimes,
    WallActivePeriodWithDates,
    WallLineVersionWithDates,
    WallOperatingPeriodWithDates,
    WallTimetableWithDates,
}
