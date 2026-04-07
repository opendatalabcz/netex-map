import type LocalTime from '@/util/localTime'
import type { LineType, TransportMode } from '@/api/model/enums'

export type JourneyDetailsScheduledStop = {
    arrival: string | null
    departure: string | null
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
    arrival: LocalTime | null
    departure: LocalTime | null
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

export type JourneyDetailsOperator = {
    relationalId: number
    publicCode: string
    legalName: string
    phone: string
    email: string
    url: string
    addressLine: string
}

export type JourneyDetailsLineVersion = {
    relationalId: number
    publicCode: string
    name: string
    shortName: string
    transportMode: TransportMode
    lineType: LineType
    detour: boolean
    operator: JourneyDetailsOperator
}

export type JourneyDetails = {
    relationalId: number
    routeId: number | null
    stops: JourneyDetailsScheduledStop[]
    transportBans: number[][] | null
    lineVersion: JourneyDetailsLineVersion
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
    lineVersion: JourneyDetailsLineVersion
    requiresOrdering: boolean
    baggageStorage: boolean
    cyclesAllowed: boolean
    lowFloorAccess: boolean
    reservationCompulsory: boolean
    reservationPossible: boolean
    snacksOnBoard: boolean
    unaccompaniedMinorAssistance: boolean
}
