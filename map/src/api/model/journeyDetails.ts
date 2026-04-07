import type LocalTime from '@/util/localTime'

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

export type JourneyDetailsTransportMode =
| 'BUS'
| 'TROLLEY_BUS'
| 'RAIL'
| 'FUNICULAR'
| 'TRAM'
| 'METRO'

export type JourneyDetailsLineType =
| 'URBAN'
| 'URBAN_SUBURBAN'
| 'INTERNATIONAL_EXCLUDING_CABOTAGE'
| 'INTERNATIONAL_INCLUDING_CABOTAGE'
| 'DOMESTIC_INTRA_REGIONAL'
| 'DOMESTIC_INTER_REGIONAL'
| 'DOMESTIC_LONG_DISTANCE'

export type JourneyDetailsLineVersion = {
    relationalId: number
    publicCode: string
    name: string
    shortName: string
    transportMode: JourneyDetailsTransportMode
    lineType: JourneyDetailsLineType
    isDetour: boolean
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
