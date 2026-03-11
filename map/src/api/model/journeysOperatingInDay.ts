import type { Route } from '@/api/model/route'
import type { LineVersion, LineVersionWithDates } from '@/api/model/lineVersion'

type DaySpecificScheduledStop = {
    name: string
    stopOnRequest: boolean
    arrival: string | null
    departure: string | null
}

type DaySpecificJourney = {
    relationalId: number
    lineVersion: LineVersion
    routeId: number | null
    schedule: DaySpecificScheduledStop[]
    nextDayFirstStopIndex: number | null
}

type JourneysOperatingInDay = {
    startingThisDay: DaySpecificJourney[]
    continuingThisDay: DaySpecificJourney[]
    routes: Route[]
}

type DaySpecificScheduledStopWithDates = {
    name: string
    stopOnRequest: boolean
    arrival: Date | null
    departure: Date | null
}

type DaySpecificJourneyWithDates = {
    relationalId: number
    lineVersion: LineVersionWithDates
    routeId: number | null
    schedule: DaySpecificScheduledStopWithDates[]
    nextDayFirstStopIndex: number | null
}

type JourneysOperatingInDayWithDates = {
    startingThisDay: DaySpecificJourneyWithDates[]
    continuingThisDay: DaySpecificJourneyWithDates[]
    routes: Route[]
}

export type {
    JourneysOperatingInDay,
    DaySpecificJourney,
    DaySpecificScheduledStop,
    JourneysOperatingInDayWithDates,
    DaySpecificJourneyWithDates,
    DaySpecificScheduledStopWithDates,
}
