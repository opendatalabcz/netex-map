<script setup lang="ts">
import type {
    JourneyDetailsScheduledStopWithTimes,
    JourneyDetailsWithTimes,
} from '@/api/model/journeyDetails'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import LineVersionLabel from '@/map/LineVersionLabel.vue'
import FacilityIcon from '@/map/FacilityIcon.vue'
import TransportBanIcons from '@/map/TransportBanIcons.vue'
import {
    displayFacilitiesForJourney,
    displayFacilitiesForCombinedStop,
    type DisplayFacilities,
} from '@/map/facilities'

const { t, d } = useI18n()

const props = defineProps<{
    journeyDetails: JourneyDetailsWithTimes
}>()
const emit = defineEmits<{
    close: []
    'stop-selected': [stopOrder: number]
    'show-timetable': []
}>()

const journeyFacilities = computed(() =>
    displayFacilitiesForJourney(
        {
            ...props.journeyDetails,
            noRoute: props.journeyDetails.routeId == null,
        },
        t,
    ),
)

const stopsWithFacilities = computed<
    {
        stop: JourneyDetailsScheduledStopWithTimes
        transportBanGroups: number[] | null | undefined
        facilities: DisplayFacilities
    }[]
>(() =>
    props.journeyDetails.stops.map((stop, stopIdx) => ({
        stop: stop,
        transportBanGroups: props.journeyDetails.transportBans?.reduce((acc, cur, groupIdx) => {
            if (cur.includes(stopIdx)) acc.push(groupIdx)
            return acc
        }, []),
        facilities: displayFacilitiesForCombinedStop(stop, t),
    })),
)
</script>

<template>
    <div class="journey-details">
        <div class="journey-header">
            <LineVersionLabel
                :transport-mode="journeyDetails.lineVersion.transportMode"
                :is-detour="journeyDetails.lineVersion.detour"
                :short-name="journeyDetails.lineVersion.shortName"
                :public-code="journeyDetails.lineVersion.publicCode"
            />
            <div class="journey-details-buttons">
                <v-btn
                    icon="mdi-timetable"
                    size="small"
                    variant="text"
                    @click="emit('show-timetable')"
                />
                <v-btn icon="mdi-close" size="small" variant="text" @click="emit('close')" />
            </div>
            {{ journeyDetails.lineVersion.operator.legalName }}
            <br />
            <div class="journey-facilities">
                <template v-for="(facility, key) in journeyFacilities" :key="key">
                    <FacilityIcon v-if="facility.active" :facility="facility" />
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
                        @click="emit('stop-selected', idx)"
                    >
                        <td class="time-mark first-td">
                            {{ d((swf.stop.departure ?? swf.stop.arrival!).toDate(), 'timeShort') }}
                        </td>
                        <td class="icon-td">
                            <div class="stop-facilities">
                                <TransportBanIcons
                                    :transport-ban-groups="swf.transportBanGroups"
                                    compact
                                />
                                <template v-for="(facility, key) in swf.facilities" :key="key">
                                    <FacilityIcon
                                        v-if="facility.active"
                                        :facility="facility"
                                        compact
                                    />
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

.line-version-label {
    padding-right: 4em;
}

.journey-details-buttons {
    position: absolute;
    top: 0;
    right: 0;
    padding-bottom: 0.125em;
    padding-left: 0.125em;
    border-bottom: 1px solid black;
    border-left: 1px solid black;
    border-bottom-left-radius: 1em;
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
    background-color: color-mix(in srgb, black 10%, white);
}

tr:hover {
    background-color: color-mix(in srgb, black 20%, white);
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
    padding-block: 0.25em;
}
</style>
