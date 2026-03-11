import type { LineVersion, LineVersionWithDates } from '@/api/model/lineVersion'
import type { OperatingPeriod, OperatingPeriodWithDates } from '@/api/model/operatingPeriod'
import type { Route } from '@/api/model/route'
import type { ScheduledStop, ScheduledStopWithTimes } from '@/api/model/scheduledStop'

type Journey = {
    relationalId: number
    externalId: string
    lineVersion: LineVersion
    journeyPatternId: string
    schedule: ScheduledStop[]
    operatingPeriods: OperatingPeriod[]
    route: Route | null
    nextDayFirstStopIndex: number | null
}

type JourneyWithDatesAndTimes = {
    relationalId: number
    externalId: string
    lineVersion: LineVersionWithDates
    journeyPatternId: string
    schedule: ScheduledStopWithTimes[]
    operatingPeriods: OperatingPeriodWithDates[]
    route: Route | null
    nextDayFirstStopIndex: number | null
}

export type { Journey, JourneyWithDatesAndTimes }
