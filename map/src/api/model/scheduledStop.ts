import type LocalTime from '@/util/localTime'

type ScheduledStop = {
    arrival: string | null
    departure: string | null
}

type ScheduledStopWithTimes = {
    arrival: LocalTime | null
    departure: LocalTime | null
}

export type { ScheduledStop, ScheduledStopWithTimes }
