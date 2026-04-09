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

const cs = {
    searchLine: 'Zadejte kód linky',
    transportModes: {
        bus: 'Autobus',
        trolleybus: 'Trolejbus',
        rail: 'Vlak',
        funicular: 'Lanová dráha',
        tram: 'Tramvaj',
        metro: 'Metro',
    },
    daysOfWeek: {
        short: {
            monday: 'Po',
            tuesday: 'Út',
            wednesday: 'St',
            thursday: 'Čt',
            friday: 'Pá',
            saturday: 'So',
            sunday: 'Ne',
        },
    },
    lineVersion: {
        detour: 'Výlukový jízdní řád',
        inDirectionTo: 'Ve směru',
        operatingPeriod: 'Fragment',
        validIn: 'Vydaný na:',
        activeIn: 'Platný v:',
        operatesRegularlyIn: 'Spoje pravidelně jezdí v:',
        alsoOperatesIn: 'Spoje také jedou v:',
        onlyOperatesIn: 'Spoje jedou pouze v:',
        doesNotOperateIn: 'Spoje nejedou v:',
    },
    journeyFacilities: {
        requiresOrdering: 'Spoj na objednávku',
        baggageStorage: 'Přeprava cestovních zavazadel',
        cyclesAllowed: 'Přeprava jízdních kol',
        lowFloorAccess: 'Bezbariérový přístup',
        reservationCompulsory: 'Nutné zakoupit místenku',
        reservationPossible: 'Možnost zakoupit místenku',
        snacksOnBoard: 'Občerstvení ve voze',
        unaccompaniedMinorAssistance: 'Částečně bezbariérový přístup, nutná dopomoc průvodce',
    },
    stopFacilities: {
        transportBan: 'Přeprava mezi shodně označenými zastávkami není povolena',
        onlyForBoarding: 'Zastávka je určena pouze pro nástup',
        onlyForAlighting: 'Zastávka je určena pouze pro výstup',
        requiresOrdering: 'Zastávka na objednávku',
        stopOnRequest: 'Zastávka na znamení',
        bistro: 'Občerstvení v zastávce',
        borderCrossing: 'Hraniční přechod, není zřízena zastávka',
        displaysForVisuallyImpaired: 'Úprava pro osoby se zrakovým postižením',
        lowFloorAccess: 'Bezbariérový přístup',
        parkAndRidePark: 'V okolí zastávky je P+R parkování',
        suitableForHeavilyDisabled: 'Zastávka je vhodná pro osoby s těžkým postižením',
        toilet: 'V zastávce se nachází toaleta',
        wheelChairAccessToilet: 'V zastávce se nachází toaleta s bezbariérovým přístupem',
    },
}

export { cs, csPluralization, csNumberFormats, csDatetimeFormats }
