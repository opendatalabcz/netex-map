import type LocalTime from '@/util/localTime'

export type JourneyDetailsScheduledStop = {
    arrival: string
    departure: string
    distanceToNextStop: number
    forBoarding: boolean
    forAlighting: boolean
    requiresOrdering: boolean
    stopOnRequest: boolean
    tariffZone: string | null
    name: string
    bistro: boolean
    borderCrossing: boolean
    displaysForVisuallyImpaired: boolean
    lowFloorAccess: boolean
    parkAndRidePark: boolean
    suitableForHeavilyDisabled: boolean
    toilet: boolean
    wheelChairAccessToilet: boolean
    otherTransportModes: string | null
}

export type JourneyDetailsScheduledStopWithTimes = {
    arrival: LocalTime
    departure: LocalTime
    distanceToNextStop: number
    forBoarding: boolean
    forAlighting: boolean
    requiresOrdering: boolean
    stopOnRequest: boolean
    tariffZone: string | null
    name: string
    bistro: boolean
    borderCrossing: boolean
    displaysForVisuallyImpaired: boolean
    lowFloorAccess: boolean
    parkAndRidePark: boolean
    suitableForHeavilyDisabled: boolean
    toilet: boolean
    wheelChairAccessToilet: boolean
    otherTransportModes: string | null
}

export type JourneyDetails = {
    relationalId: number
    routeId: number | null
    stops: JourneyDetailsScheduledStop[]
    transportBans: number[][] | null
    requiresOrdering: boolean
    baggageStorage: boolean
    cyclesAllowed: boolean
    lowFloorAccess: boolean
    reservationCompulsory: boolean
    reservationPossible: boolean
    snacksOnBoard: boolean
    unaccompaniedMinorAssistance: boolean
}

export type JourneyDetailsWithTimes = {
    relationalId: number
    routeId: number | null
    stops: JourneyDetailsScheduledStopWithTimes[]
    transportBans: number[][] | null
    requiresOrdering: boolean
    baggageStorage: boolean
    cyclesAllowed: boolean
    lowFloorAccess: boolean
    reservationCompulsory: boolean
    reservationPossible: boolean
    snacksOnBoard: boolean
    unaccompaniedMinorAssistance: boolean
}
