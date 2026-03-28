function csPluralization(choice: number, choicesLength: number): number {
    // Special case for numbers between 2 and 4
    const some = choice > 1 && choice < 5
    if (some && choicesLength >= 2) {
        return 1
    }

    // Special case for number 1
    if (choice === 1 && choicesLength >= 3) {
        return 2
    }

    // Typical plural
    return 0
}

const csNumberFormats: Record<string, Intl.NumberFormatOptions> = {
    decimal: {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 12,
    },
}

const csDatetimeFormats: Record<string, Intl.DateTimeFormatOptions> = {
    short: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
    },
    long: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
    timeShort: {
        hour: 'numeric',
        minute: 'numeric',
    },
}

const cs = {}

export { cs, csPluralization, csNumberFormats, csDatetimeFormats }
