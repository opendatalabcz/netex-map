import type { LineVersion, LineVersionWithDates } from '@/api/model/lineVersion'
import type { OperatingPeriod, OperatingPeriodWithDates } from '@/api/model/operatingPeriod'
import type { Route } from '@/api/model/route'
import type { ScheduledStop, ScheduledStopWithTimes } from '@/api/model/scheduledStop'

type Journey = {
    relationalId: number
    journeyNumber: string
    lineVersion: LineVersion
    schedule: ScheduledStop[]
    operatingPeriod: OperatingPeriod
    route: Route | null
    nextDayFirstStopIndex: number | null
    beginTime: string
    endTime: string
    timezone: string
}

type JourneyWithDatesAndTimes = {
    relationalId: number
    journeyNumber: string
    lineVersion: LineVersionWithDates
    schedule: ScheduledStopWithTimes[]
    operatingPeriod: OperatingPeriodWithDates
    route: Route | null
    nextDayFirstStopIndex: number | null
    beginTime: string
    endTime: string
    timezone: string
}

export type { Journey, JourneyWithDatesAndTimes }
