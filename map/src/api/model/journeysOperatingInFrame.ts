import type { EncodedRoute } from '@/api/model/encodedRoute'
import type { LineType } from '@/api/model/enums'

export type FrameScheduledStop = {
    arrival: string | null
    departure: string | null
}

export type FrameJourney = {
    relationalId: number
    lineVersionId: number
    routeId: number
    schedule: FrameScheduledStop[]
    nextDayFirstStopIndex: number | null
    fromPreviousDay: boolean
}

export type FrameLineVersion = {
    relationalId: number
    lineType: LineType
}

export type JourneysOperatingInFrameResult = {
    journeys: FrameJourney[]
    routes: EncodedRoute[]
    lineVersions: FrameLineVersion[]
}

export type FrameScheduledStopWithDates = {
    arrival: Date | null
    departure: Date | null
}

export type FrameJourneyWithDates = {
    relationalId: number
    lineVersionId: number
    routeId: number
    schedule: FrameScheduledStopWithDates[]
    nextDayFirstStopIndex: number | null
    fromPreviousDay: boolean
}
