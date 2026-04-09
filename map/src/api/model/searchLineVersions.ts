import type { LineType, TransportMode } from '@/api/model/enums'

export type SearchLineVersionsActivePeriod = {
    fromDate: string
    toDate: string
}

export type SearchLineVersionsActivePeriodWithDates = {
    fromDate: Date
    toDate: Date
}

export type SearchLineVersionsOperator = {
    relationalId: number
    publicCode: string
    legalName: string
    phone: string
    email: string
    url: string
    addressLine: string
}

export type SearchLineVersion = {
    relationalId: number
    publicCode: string
    name: string
    shortName: string
    transportMode: TransportMode
    lineType: LineType
    detour: boolean
    validFrom: string
    validTo: string
    operator: SearchLineVersionsOperator
    activePeriods: SearchLineVersionsActivePeriod[]
}

export type SearchLineVersionWithDates = {
    relationalId: number
    publicCode: string
    name: string
    shortName: string
    transportMode: TransportMode
    lineType: LineType
    detour: boolean
    validFrom: Date
    validTo: Date
    operator: SearchLineVersionsOperator
    activePeriods: SearchLineVersionsActivePeriodWithDates[]
}
