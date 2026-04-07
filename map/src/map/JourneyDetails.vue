<script setup lang="ts">
import type { JourneyDetailsScheduledStopWithTimes, JourneyDetailsWithTimes } from '@/api/model/journeyDetails'
import { computed } from 'vue';
import { useI18n } from 'vue-i18n'

const { t, d } = useI18n()

const props = defineProps<{
    journeyDetails: JourneyDetailsWithTimes
}>()
const emit = defineEmits<{
    close: [],
    stopSelected: [stopOrder: number],
}>()

const journeyFacilities = computed(() => ({
    requiresOrdering: {
        icon: 'mdi-phone-alert',
        tooltip: t('journeyFacilities.requiresOrdering'),
        active: props.journeyDetails.requiresOrdering,
    },
    baggageStorage: {
        icon: 'mdi-bag-suitcase',
        tooltip: t('journeyFacilities.baggageStorage'),
        active: props.journeyDetails.baggageStorage,
    },
    cyclesAllowed: {
        icon: 'mdi-bicycle',
        tooltip: t('journeyFacilities.cyclesAllowed'),
        active: props.journeyDetails.cyclesAllowed,
    },
    lowFloorAccess: {
        icon: 'mdi-wheelchair',
        tooltip: t('journeyFacilities.lowFloorAccess'),
        active: props.journeyDetails.lowFloorAccess,
    },
    reservationCompulsory: {
        icon: 'mdi-alpha-r-box-outline',
        tooltip: t('journeyFacilities.reservationCompulsory'),
        active: props.journeyDetails.reservationCompulsory,
    },
    reservationPossible: {
        icon: 'mdi-alpha-r',
        tooltip: t('journeyFacilities.reservationPossible'),
        active: props.journeyDetails.reservationPossible && !props.journeyDetails.reservationCompulsory,
    },
    snacksOnBoard: {
        icon: 'mdi-silverware-variant',
        tooltip: t('journeyFacilities.snacksOnBoard'),
        active: props.journeyDetails.snacksOnBoard,
    },
    unaccompaniedMinorAssistance: {
        icon: 'mdi-human-wheelchair',
        tooltip: t('journeyFacilities.unaccompaniedMinorAssistance'),
        active: props.journeyDetails.unaccompaniedMinorAssistance,
    },
}))

const stopsWithFacilities = computed<{
    stop: JourneyDetailsScheduledStopWithTimes,
    transportBanGroups: number[] | null | undefined,
    facilities: Record<string, {
        icon?: string,
        iconProps?: object,
        text?: string,
        tooltip: string,
        active: boolean,
    }>,
}[]>(() => props.journeyDetails.stops.map((stop) => ({
    stop: stop,
    transportBanGroups: props.journeyDetails.transportBans?.reduce((acc, cur, idx) => {
        if (cur.includes(idx)) acc.push(idx)
        return acc
    }, []),
    facilities: {
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
            active: stop.displaysForVisuallyImpaired,
        },
        suitableForHeavilyDisabled: {
            icon: 'mdi-human-white-cane',
            iconProps: {
                style: 'margin-inline-start: -0.25em;',
                size: 'x-small'
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
                size: 'x-small'
            },
            text: 'WC',
            tooltip: t('stopFacilities.wheelChairAccessToilet'),
            active: stop.wheelChairAccessToilet,
        },
    },
})))

const transportMode = computed(() => {
    switch (props.journeyDetails.lineVersion.transportMode) {
        case 'BUS':
            return {
                icon: 'mdi-bus',
                text: t('transportModes.bus')
            }
        case 'TROLLEY_BUS':
            return {
                icon: 'mdi-bus',
                text: t('transportModes.trolleybus')
            }
        case 'RAIL':
            return {
                icon: 'mdi-train',
                text: t('transportModes.rail')
            }
        case 'FUNICULAR':
            return {
                icon: 'mdi-gondola',
                text: t('transportModes.funicular')
            }
        case 'TRAM':
            return {
                icon: 'mdi-tram',
                text: t('transportModes.tram')
            }
        case 'METRO':
            return {
                icon: 'mdi-subway-variant',
                text: t('transportModes.metro')
            }
        default:
            return {
                icon: 'mdi-bus',
                text: t('transportModes.bus')
            }
    }
})

</script>

<template>
    <div class="journey-details">
        <div class="journey-header">
            <v-tooltip
                v-if="journeyDetails.lineVersion.isDetour"
                :text="t('journeyFacilities.detour')"
            >
                <template #activator="tooltipProps">
                    <v-icon
                        v-bind="tooltipProps.props"
                        icon="mdi-alert"
                        size="small"
                        color="warning"
                    />
                </template>
            </v-tooltip>
            <v-icon :icon="transportMode.icon"/>
            {{ transportMode.text }}
            {{ journeyDetails.lineVersion.shortName }}
            <span
                v-if="journeyDetails.lineVersion.shortName !== journeyDetails.lineVersion.publicCode"
                class="public-code"
            >
                {{ journeyDetails.lineVersion.publicCode }}
            </span>
            <v-spacer />
            <v-btn
                class="journey-details-close-button"
                icon="mdi-close"
                size="small"
                variant="text"
                @click="emit('close')"
            />
            {{ journeyDetails.lineVersion.operator.legalName }}
            <br/>
            <div
                v-if="Object.values(journeyFacilities).some((facility) => facility.active)"
                class="journey-facilities"
            >
                <template
                    v-for="(facility, key) in journeyFacilities"
                    :key="key"
                >
                    <v-tooltip
                        v-if="facility.active"
                        :text="facility.tooltip"
                    >
                        <template #activator="tooltipProps">
                            <v-icon
                                v-bind="tooltipProps.props"
                                :icon="facility.icon"
                            />
                        </template>
                    </v-tooltip>
                </template>
            </div>
            <v-divider class="mt-1 mb-0" opacity="1" />
        </div>
        <div class="journey-table-wrapper">
            <table>
                <tbody>
                    <tr
                        v-for="(swf, idx) in stopsWithFacilities"
                        :key="idx"
                        v-ripple
                        @click="emit('stopSelected', idx)"
                    >
                        <td class="time-mark first-td">{{ d((swf.stop.departure ?? swf.stop.arrival!).toDate(), 'timeShort') }}</td>
                        <td class="icon-td">
                            <div
                                v-if="Object.values(swf.facilities).some((facility) => facility.active)"
                                class="stop-facilities"
                            >
                                <v-tooltip
                                    v-for="banGroup in swf.transportBanGroups"
                                    :key="banGroup"
                                    :text="t('stopFacilities.transportBan')"
                                >
                                    <template #activator="tooltipProps">
                                        <span
                                            v-bind="tooltipProps.props"
                                            class="stop-facility-text"
                                        >
                                            §<sub>{{banGroup + 1}}</sub>
                                        </span>
                                    </template>
                                </v-tooltip>
                                <template
                                    v-for="(facility, key) in swf.facilities"
                                    :key="`${idx}-${key}`"
                                >
                                    <v-tooltip
                                        v-if="facility.active"
                                        :text="facility.tooltip"
                                    >
                                        <template #activator="tooltipProps">
                                            <span
                                                v-if="facility.text"
                                                v-bind="tooltipProps.props"
                                                class="stop-facility-text"
                                            >
                                                {{ facility.text }}
                                            </span>
                                            <v-icon
                                                v-if="facility.icon"
                                                size="small"
                                                v-bind="{...tooltipProps.props, ...facility.iconProps}"
                                                :icon="facility.icon"
                                            />
                                        </template>
                                    </v-tooltip>
                                </template>
                            </div>
                        </td>
                        <td class="last-td">{{ swf.stop.name }}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>

<style scoped>
.journey-details {
    display: flex;
    flex-direction: column;
    min-height: 0;
    padding: 1em;
}

.journey-table-wrapper {
    overflow-y: auto;
    min-height: 0;
}

.journey-header {
    flex: 0 0 auto;
    font-weight: bold;
    font-size: 1.15em;
}

.journey-details-close-button {
    position: absolute;
    top: 0.25em;
    right: 0.25em;
}

.public-code {
    font-size: 0.75em;
    opacity: 0.6;
}

.time-mark {
    text-align: right;
}

.journey-facilities {
    display: flex;
    gap: 0.25em;
}

.stop-facilities {
    display: flex;
    gap: 0.125em;
    align-items: center;
}

.stop-facility-text {
    line-height: 1em;
    font-size: 0.9em;
    font-weight: bold;
}

table {
    border-collapse: collapse;
    width: 100%;
}

tr:nth-child(even) {
    background-color: color-mix(in srgb, currentColor 10%, transparent);
}

tr:hover {
    background-color: color-mix(in srgb, currentColor 20%, transparent);
    cursor: pointer;
}

.first-td {
    padding-inline-start: 0.5em;
}

.icon-td {
    padding-inline: 0.5em;
}

.last-td {
    padding-inline-end: 0.5em;
}

td {
    padding-block: 0.5em;
}
</style>
