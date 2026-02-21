import type LocalTime from "@/util/localTime"

type ScheduledStop = {
    name: string,
    stopOnRequest: boolean,
    arrival: string | null,
    departure: string | null,
}

type ScheduledStopWithTimes = {
    name: string,
    stopOnRequest: boolean,
    arrival: LocalTime | null,
    departure: LocalTime | null,
}

export type { ScheduledStop, ScheduledStopWithTimes }
