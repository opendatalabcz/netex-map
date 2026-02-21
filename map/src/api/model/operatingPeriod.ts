type OperatingPeriod = {
    timezone: string,
    fromDate: string,
    toDate: string,
    validDays: string,
}

type OperatingPeriodWithDates = {
    fromDate: Date,
    toDate: Date,
    validDays: string,
}

export type { OperatingPeriod, OperatingPeriodWithDates }
