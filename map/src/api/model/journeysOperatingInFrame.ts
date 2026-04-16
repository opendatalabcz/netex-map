import type { EncodedRoute } from "@/api/model/encodedRoute"

export type MapScheduledStop = {
    arrival: string | null
    departure: string | null
}

export type MapJourney = {
    relationalId: number
    lineVersionId: number
    routeId: number
    schedule: MapScheduledStop[]
    nextDayFirstStopIndex: number | null
    fromPreviousDay: boolean
}

export type JourneysOperatingInFrame = {
    journeys: MapJourney[]
    routes: EncodedRoute[]
}

export type MapScheduledStopWithDates = {
    arrival: Date | null
    departure: Date | null
}

export type MapJourneyWithDates = {
    relationalId: number
    lineVersionId: number
    routeId: number
    schedule: MapScheduledStopWithDates[]
    nextDayFirstStopIndex: number | null
    fromPreviousDay: boolean
}
