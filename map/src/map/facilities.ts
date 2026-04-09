export type StopFacilities = {
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

export type CombinedStopFacilities = StopFacilities & {
    forBoarding: boolean
    forAlighting: boolean
    requiresOrdering: boolean
    stopOnRequest: boolean
}

export type JourneyFacilities = {
    requiresOrdering: boolean
    baggageStorage: boolean
    cyclesAllowed: boolean
    lowFloorAccess: boolean
    reservationCompulsory: boolean
    reservationPossible: boolean
    snacksOnBoard: boolean
    unaccompaniedMinorAssistance: boolean
}

export type DisplayFacility = {
    icon?: string
    iconProps?: object
    text?: string
    tooltip: string
    active: boolean
}

export type DisplayFacilities = Record<string, DisplayFacility>

export function displayFacilitiesForJourney(
    journey: JourneyFacilities,
    t: (key: string) => string,
): DisplayFacilities {
    return {
        requiresOrdering: {
            icon: 'mdi-phone-alert',
            tooltip: t('journeyFacilities.requiresOrdering'),
            active: journey.requiresOrdering,
        },
        baggageStorage: {
            icon: 'mdi-bag-suitcase',
            tooltip: t('journeyFacilities.baggageStorage'),
            active: journey.baggageStorage,
        },
        cyclesAllowed: {
            icon: 'mdi-bicycle',
            tooltip: t('journeyFacilities.cyclesAllowed'),
            active: journey.cyclesAllowed,
        },
        lowFloorAccess: {
            icon: 'mdi-wheelchair',
            tooltip: t('journeyFacilities.lowFloorAccess'),
            active: journey.lowFloorAccess && !journey.unaccompaniedMinorAssistance,
        },
        reservationCompulsory: {
            icon: 'mdi-alpha-r-box-outline',
            tooltip: t('journeyFacilities.reservationCompulsory'),
            active: journey.reservationCompulsory,
        },
        reservationPossible: {
            icon: 'mdi-alpha-r',
            tooltip: t('journeyFacilities.reservationPossible'),
            active: journey.reservationPossible && !journey.reservationCompulsory,
        },
        snacksOnBoard: {
            icon: 'mdi-silverware-variant',
            tooltip: t('journeyFacilities.snacksOnBoard'),
            active: journey.snacksOnBoard,
        },
        unaccompaniedMinorAssistance: {
            icon: 'mdi-human-wheelchair',
            tooltip: t('journeyFacilities.unaccompaniedMinorAssistance'),
            active: journey.unaccompaniedMinorAssistance,
        },
    }
}

export function displayFacilitiesForStop(
    stop: StopFacilities,
    t: (key: string) => string,
): DisplayFacilities {
    return {
        bistro: {
            icon: 'mdi-silverware-variant',
            tooltip: t('stopFacilities.bistro'),
            active: stop.bistro,
        },
        borderCrossing: {
            text: 'CLO',
            tooltip: t('stopFacilities.borderCrossing'),
            active: stop.borderCrossing,
        },
        displaysForVisuallyImpaired: {
            icon: 'mdi-human-white-cane',
            tooltip: t('stopFacilities.displaysForVisuallyImpaired'),
            active: stop.displaysForVisuallyImpaired && !stop.suitableForHeavilyDisabled,
        },
        suitableForHeavilyDisabled: {
            icon: 'mdi-human-white-cane',
            iconProps: {
                style: 'margin-inline-start: -0.25em;',
                size: 'x-small',
            },
            text: 'EX',
            tooltip: t('stopFacilities.suitableForHeavilyDisabled'),
            active: stop.suitableForHeavilyDisabled,
        },
        lowFloorAccess: {
            icon: 'mdi-wheelchair',
            tooltip: t('stopFacilities.lowFloorAccess'),
            active: stop.lowFloorAccess,
        },
        parkAndRidePark: {
            text: 'P+R',
            tooltip: t('stopFacilities.parkAndRidePark'),
            active: stop.parkAndRidePark,
        },
        toilet: {
            text: 'WC',
            tooltip: t('stopFacilities.toilet'),
            active: stop.toilet && !stop.wheelChairAccessToilet,
        },
        wheelChairAccessToilet: {
            icon: 'mdi-wheelchair',
            iconProps: {
                style: 'margin-inline-start: -0.25em;',
                size: 'x-small',
            },
            text: 'WC',
            tooltip: t('stopFacilities.wheelChairAccessToilet'),
            active: stop.wheelChairAccessToilet,
        },
    }
}

export function displayFacilitiesForCombinedStop(
    stop: CombinedStopFacilities,
    t: (key: string) => string,
): DisplayFacilities {
    return {
        ...displayFacilitiesForStop(stop, t),
        onlyForBoarding: {
            icon: 'mdi-circle-half',
            iconProps: {
                style: 'rotate: 180deg;',
            },
            tooltip: t('stopFacilities.onlyForBoarding'),
            active: stop.forBoarding && !stop.forAlighting,
        },
        onlyForAlighting: {
            icon: 'mdi-circle-half',
            tooltip: t('stopFacilities.onlyForAlighting'),
            active: stop.forAlighting && !stop.forBoarding,
        },
        requiresOrdering: {
            icon: 'mdi-phone-alert',
            tooltip: t('stopFacilities.requiresOrdering'),
            active: stop.requiresOrdering,
        },
        stopOnRequest: {
            icon: 'mdi-bell',
            tooltip: t('stopFacilities.stopOnRequest'),
            active: stop.stopOnRequest,
        },
    }
}
