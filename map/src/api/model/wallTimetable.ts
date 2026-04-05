import type LocalTime from '@/util/localTime'

export type WallScheduledStop = {
    arrival: string | null
    departure: string | null
}

export type WallOperatingDays = {
    monday: boolean
    tuesday: boolean
    wednesday: boolean
    thursday: boolean
    friday: boolean
    saturday: boolean
    sunday: boolean
}

export type WallOperationExceptionType = 'ALSO_OPERATES' | 'DOES_NOT_OPERATE'

export type WallTariffStop = {
    tariffZone: string | null
    stopId: number
}

export type WallActivePeriod = {
    fromDate: string
    toDate: string
}

export type LineType =
    | 'URBAN'
    | 'URBAN_SUBURBAN'
    | 'INTERNATIONAL_EXCLUDING_CABOTAGE'
    | 'INTERNATIONAL_INCLUDING_CABOTAGE'
    | 'DOMESTIC_INTRA_REGIONAL'
    | 'DOMESTIC_INTER_REGIONAL'
    | 'DOMESTIC_LONG_DISTANCE'

export type WallOperator = {
    relationalId: number
    publicCode: string
    legalName: string
    phone: string
    email: string
    url: string
    addressLine: string
}

export type WallStop = {
    relationalId: number
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

export type WallLineVersion = {
    relationalId: number
    publicCode: string
    name: string
    shortName: string
    transportMode: string
    lineType: LineType
    isDetour: boolean
    operator: WallOperator
    activePeriods: WallActivePeriod[]
    tariffStops: WallTariffStop[]
    stops: WallStop[]
}

export type WallJourney = {
    relationalId: number
    schedule: WallScheduledStop[]
    requiresOrdering: boolean
    baggageStorage: boolean
    cyclesAllowed: boolean
    lowFloorAccess: boolean
    reservationCompulsory: boolean
    reservationPossible: boolean
    snacksOnBoard: boolean
    unaccompaniedMinorAssistance: boolean
}

export type WallJourneyWithTimes = {
    relationalId: number
    schedule: WallScheduledStopWithTimes[]
    requiresOrdering: boolean
    baggageStorage: boolean
    cyclesAllowed: boolean
    lowFloorAccess: boolean
    reservationCompulsory: boolean
    reservationPossible: boolean
    snacksOnBoard: boolean
    unaccompaniedMinorAssistance: boolean
}

export type WallJourneyPatternStop = {
    tariffOrder: number
    distanceToNextStop: number
    forBoarding: boolean
    forAlighting: boolean
    requiresOrdering: boolean
    stopOnRequest: boolean
}

export type JourneyDirection = 'OUTBOUND' | 'INBOUND' | 'CLOCKWISE' | 'ANTICLOCKWISE'

export type WallJourneyPattern = {
    patternNumber: number
    direction: JourneyDirection
    stops: WallJourneyPatternStop[]
    transportBans: number[][] | null
    routeId: number
}

export type WallOperatingPeriod = {
    operatingDays: WallOperatingDays
    operationExceptions: Record<WallOperationExceptionType, string[]>
    journeys: WallJourney[]
}

export type WallTimetable = {
    lineVersion: WallLineVersion
    operatingPeriods: WallOperatingPeriod[]
    journeyPatterns: WallJourneyPattern[]
}

export type WallScheduledStopWithTimes = {
    arrival: LocalTime | null
    departure: LocalTime | null
}

export type WallActivePeriodWithDates = {
    fromDate: Date
    toDate: Date
}

export type WallLineVersionWithDates = {
    relationalId: number
    publicCode: string
    name: string
    shortName: string
    transportMode: string
    lineType: LineType
    isDetour: boolean
    operator: WallOperator
    activePeriods: WallActivePeriodWithDates[]
    tariffStops: WallTariffStop[]
    stops: WallStop[]
}

export type WallOperatingPeriodWithDates = {
    operatingDays: WallOperatingDays
    operationExceptions: Map<WallOperationExceptionType, Date[]>
    journeys: WallJourneyWithTimes[]
}

export type WallTimetableWithDates = {
    lineVersion: WallLineVersionWithDates
    operatingPeriods: WallOperatingPeriodWithDates[]
    journeyPatterns: WallJourneyPattern[]
}
