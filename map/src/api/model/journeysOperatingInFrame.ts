type MapRawRoute = {
    relationalId: number
    pointSequence: string
    totalDistance: number
    routeStops: number[]
}

type MapRoute = {
    relationalId: number
    pointSequence: GeoJSON.LineString
    totalDistance: number
    routeStops: number[]
}

type MapLineVersion = {
    relationalId: number
    publicCode: string
    transportMode: string
}

type MapScheduledStop = {
    arrival: string | null
    departure: string | null
}

type MapJourney = {
    relationalId: number
    lineVersionId: number
    routeId: number
    schedule: MapScheduledStop[]
    nextDayFirstStopIndex: number | null
    fromPreviousDay: boolean
}

type JourneysOperatingInFrame = {
    journeys: MapJourney[]
    routes: MapRawRoute[]
}

type MapScheduledStopWithDates = {
    arrival: Date | null
    departure: Date | null
}

type MapJourneyWithDates = {
    relationalId: number
    lineVersionId: number
    routeId: number
    schedule: MapScheduledStopWithDates[]
    nextDayFirstStopIndex: number | null
    fromPreviousDay: boolean
}

export type {
    JourneysOperatingInFrame,
    MapJourney,
    MapScheduledStop,
    MapLineVersion,
    MapRawRoute,
    MapJourneyWithDates,
    MapScheduledStopWithDates,
    MapRoute,
}
