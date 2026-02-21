type LineVersion = {
    lineId: string,
    publicCode: string,
    name: string,
    shortName: string,
    transportMode: string,
    validFrom: string,
    validTo: string,
    isDetour: boolean,
}

type LineVersionWithDates = {
    lineId: string,
    publicCode: string,
    name: string,
    shortName: string,
    transportMode: string,
    validFrom: Date,
    validTo: Date,
    isDetour: boolean,
}

export type { LineVersion, LineVersionWithDates }
