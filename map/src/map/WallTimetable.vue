<script setup lang="ts">
import type { WallOperatingPeriodWithDates, WallTimetableWithDates } from '@/api/model/wallTimetable'
import LineVersionLabel from '@/map/LineVersionLabel.vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { displayFacilitiesForCombinedStop, displayFacilitiesForJourney, type CombinedStopFacilities, type DisplayFacilities } from '@/map/facilities'
import FacilityIcon from '@/map/FacilityIcon.vue'
import TransportBanIcons from '@/map/TransportBanIcons.vue'
import type { JourneyDirection } from '@/api/model/enums'
import type LocalTime from '@/util/localTime'

const { t, d } = useI18n()

const emit = defineEmits<{
    close: []
}>()
const props = defineProps<{
    wallTimetable: WallTimetableWithDates
}>()

const stopsMap = computed(
    () => new Map(props.wallTimetable.lineVersion.stops.map((s) => [s.relationalId, s])),
)
const journeyPatternsMap = computed(
    () => new Map(props.wallTimetable.journeyPatterns.map((j) => [j.patternNumber, j])),
)
const operatingPeriodsMap = computed(
    () => new Map(props.wallTimetable.operatingPeriods.map((o) => [o.relationalId, o]))
)

type DisplayStop = {
    stopName: string
    facilities: DisplayFacilities
    banGroups: number[]
}
const tariffStopList = computed<DisplayStop[]>(() => {
    const journeyPatterns = props.wallTimetable.journeyPatterns
    const transportBanGroups = Array.from(new Map(
        journeyPatterns
            .filter((jp) => jp.transportBans != null && (jp.direction === 'OUTBOUND' || jp.direction === 'CLOCKWISE'))
            .flatMap((journeyPattern) =>
                journeyPattern.transportBans!.map((banGroup) =>
                    banGroup.map((journeyPatternStopIdx) => journeyPattern.stops[journeyPatternStopIdx]!.tariffOrder)
                )
            ).map((banGroup) => [banGroup.join(','), banGroup])
    ).values())
    const tariffStops = props.wallTimetable.lineVersion.tariffStops
    return tariffStops.map((tariffStop, tariffStopIdx) => {
        const stop = stopsMap.value.get(tariffStop.stopId)!
        const facilitiesFromJourneyPatterns = journeyPatterns.reduce((acc, journeyPattern) => {
            for (const journeyPatternStop of journeyPattern.stops) {
                if (journeyPatternStop.tariffOrder !== tariffStopIdx) continue
                if (journeyPatternStop.forBoarding) acc.forBoarding = true
                if (journeyPatternStop.forAlighting) acc.forAlighting = true
                if (journeyPatternStop.requiresOrdering) acc.requiresOrdering = true
                if (journeyPatternStop.stopOnRequest) acc.stopOnRequest = true
                break
            }
            return acc
        }, {
            forBoarding: false,
            forAlighting: false,
            requiresOrdering: false,
            stopOnRequest: false,
        })
        const combinedStop: CombinedStopFacilities = {
            forBoarding: facilitiesFromJourneyPatterns.forBoarding,
            forAlighting: facilitiesFromJourneyPatterns.forAlighting,
            requiresOrdering: facilitiesFromJourneyPatterns.requiresOrdering,
            stopOnRequest: facilitiesFromJourneyPatterns.stopOnRequest,
            bistro: stop.bistro,
            borderCrossing: stop.borderCrossing,
            displaysForVisuallyImpaired: stop.displaysForVisuallyImpaired,
            lowFloorAccess: stop.lowFloorAccess,
            parkAndRidePark: stop.parkAndRidePark,
            suitableForHeavilyDisabled: stop.suitableForHeavilyDisabled,
            toilet: stop.toilet,
            wheelChairAccessToilet: stop.wheelChairAccessToilet,
            otherTransportModes: stop.otherTransportModes,
        }
        return {
            stopName: stop.name,
            facilities: displayFacilitiesForCombinedStop(combinedStop, t),
            banGroups: transportBanGroups.reduce((acc, cur, groupIdx) => {
                if (cur.includes(tariffStopIdx)) acc.push(groupIdx)
                return acc
            }, [])
        }
    })
})
const reversedTariffStopList = computed(() => {
    return [...tariffStopList.value].reverse()
})


type DisplayJourney = {
    facilities: DisplayFacilities
    schedule: (LocalTime | null)[]
}
type DisplayOperatingPeriod = WallOperatingPeriodWithDates & {
    journeys: DisplayJourney[]
    regularDays: string[]
    alsoOperatesIn: Date[]
    doesNotOperateIn: Date[]
}
function getDisplayOperatingPeriodsForDirection(direction: JourneyDirection): DisplayOperatingPeriod[] {
    const journeysByOperatingPeriod = Array.from(props.wallTimetable.journeys.get(direction)!.entries())
    return journeysByOperatingPeriod.map(([operatingPeriodId, journeys]) => {
        const operatingPeriod = operatingPeriodsMap.value.get(operatingPeriodId)!
        const res = {...operatingPeriod} as DisplayOperatingPeriod
        res.regularDays = Object.entries(operatingPeriod.operatingDays)
            .filter(([, active]) => active)
            .map(([day,]) => day)
        res.alsoOperatesIn = []
        res.doesNotOperateIn = []
        for (const [operationExceptionType, days] of operatingPeriod.operationExceptions) {
            switch(operationExceptionType) {
                case 'ALSO_OPERATES': {
                    days.forEach((d) => res.alsoOperatesIn.push(d))
                    break
                }
                case 'DOES_NOT_OPERATE': {
                    days.forEach((d) => res.doesNotOperateIn.push(d))
                    break
                }
            }
        }
        res.journeys = []
        for (const journey of journeys) {
            const displayJourney: DisplayJourney = {
                facilities: displayFacilitiesForJourney(journey, t),
                schedule: [],
            }
            res.journeys.push(displayJourney)
            const journeyPattern = journeyPatternsMap.value.get(journey.patternNumber)!
            let journeyPatternIdx = 0
            const reverseOrder = direction !== 'CLOCKWISE' && direction !== 'OUTBOUND'
            const tariffStopCount = props.wallTimetable.lineVersion.tariffStops.length
            let tariffIdx = reverseOrder ? tariffStopCount - 1 : 0
            while(reverseOrder ? tariffIdx >= 0 : tariffIdx < tariffStopCount) {
                const patternStop = journeyPattern.stops[journeyPatternIdx]
                if (patternStop != null && patternStop.tariffOrder === tariffIdx) {
                    const scheduledStop = journey.schedule[journeyPatternIdx]!
                    displayJourney.schedule.push(scheduledStop.departure ?? scheduledStop.arrival!)
                    journeyPatternIdx += 1
                } else {
                    displayJourney.schedule.push(null)
                }
                tariffIdx += reverseOrder ? -1 : 1
            }
        }
        return res
    })
}
const directionDisplayTabs = computed(() => Array.from(props.wallTimetable.journeys.keys()).map((direction) => {
    let displayStops: DisplayStop[]
    switch(direction) {
        case 'CLOCKWISE':
        case 'OUTBOUND': {
            displayStops = tariffStopList.value
            break
        }
        case 'ANTICLOCKWISE':
        case 'INBOUND': {
            displayStops = reversedTariffStopList.value
            break
        }
    }
    const text = t('lineVersion.inDirectionTo') + ' ' + displayStops[displayStops.length - 1]!.stopName
    return {
        value: direction,
        text: text,
        displayStops: displayStops,
        displayOperatingPeriods: getDisplayOperatingPeriodsForDirection(direction),
    }
}))
const activeDirectionTab = ref<JourneyDirection>(directionDisplayTabs.value[0]!.value)
const activeOperatingPeriodTab = ref<number>(0)
watch(
    activeDirectionTab,
    () => activeOperatingPeriodTab.value = directionDisplayTabs.value[0]!.displayOperatingPeriods[0]!.relationalId,
    { immediate: true }
)
const activeDisplayDirection = computed(() => directionDisplayTabs.value.find((tab) => tab.value === activeDirectionTab.value)!)

</script>

<template>
    <div class="wall-timetable">
        <div class="timetable-header">
            <div class="timetable-header-info">
                <div>
                    <LineVersionLabel
                        :transport-mode="wallTimetable.lineVersion.transportMode"
                        :is-detour="wallTimetable.lineVersion.detour"
                        :short-name="wallTimetable.lineVersion.shortName"
                        :public-code="wallTimetable.lineVersion.publicCode"
                    />
                    <v-btn
                        class="wall-timetable-close-button"
                        icon="mdi-close"
                        size="small"
                        variant="text"
                        @click="emit('close')"
                    />
                    {{ wallTimetable.lineVersion.operator.legalName }}
                </div>
                <div class="timetable-header-dates">
                    {{ t('lineVersion.validIn') }}
                    <span class="timetable-header-chips">
                        <v-chip
                            :text="`${d(wallTimetable.lineVersion.validFrom, 'short')} - ${d(wallTimetable.lineVersion.validTo, 'short')}`"
                            label
                            density="compact"
                        />
                    </span>
                    {{ t('lineVersion.activeIn') }}
                    <span class="timetable-header-chips">
                        <v-chip
                            v-for="ap in wallTimetable.lineVersion.activePeriods"
                            :key="ap.fromDate.toISOString()"
                            :text="`${d(ap.fromDate, 'short')} - ${d(ap.toDate, 'short')}`"
                            label
                            density="compact"
                        />
                    </span>
                </div>
            </div>
            <v-divider class="mt-1" opacity="1" />
            <v-tabs
                v-if="directionDisplayTabs.length > 1"
                v-model="activeDirectionTab"
                grow
            >
                <v-tab
                    v-for="tab in directionDisplayTabs"
                    :key="tab.value"
                    :text="tab.text"
                    :value="tab.value"
                    selected-class="selected-tab"
                />
            </v-tabs>
            <v-divider opacity="0.25" />
            <v-tabs
                v-if="activeDisplayDirection.displayOperatingPeriods.length > 1"
                v-model="activeOperatingPeriodTab"
                grow
            >
                <v-tab
                    v-for="(op, idx) in activeDisplayDirection.displayOperatingPeriods"
                    :key="op.relationalId"
                    :value="op.relationalId"
                    :text="t('lineVersion.operatingPeriod') + ' ' + (idx + 1)"
                    selected-class="selected-tab"
                />
            </v-tabs>
            <v-divider opacity="1" />
        </div>
        <div class="timetable-body-wrapper">
            <v-tabs-window v-model="activeDirectionTab">
                <v-tabs-window-item
                    v-for="tab in directionDisplayTabs"
                    :key="tab.value"
                    :value="tab.value"
                >
                    <v-tabs-window v-model="activeOperatingPeriodTab">
                        <v-tabs-window-item
                            v-for="op in tab.displayOperatingPeriods"
                            :key="op.relationalId"
                            :value="op.relationalId"
                            class="passing-times"
                        >
                            <div class="operation-time">
                                <span v-if="op.regularDays.length > 0">
                                    {{ t('lineVersion.operatesRegularlyIn') }}
                                    {{ op.regularDays.map((day) => t(`daysOfWeek.short.${day}`)).join(', ') }}
                                </span>
                                <span v-if="op.alsoOperatesIn.length > 0">
                                    <span class="operating-days-chips">
                                        {{  t(op.regularDays.length > 0 ? 'lineVersion.alsoOperatesIn' : 'lineVersion.onlyOperatesIn') }}
                                        <v-chip
                                            v-for="day in op.alsoOperatesIn"
                                            :key="day.toISOString()"
                                            :text="d(day, 'short')"
                                            label
                                            density="compact"
                                        />
                                    </span>
                                </span>
                                <span v-if="op.doesNotOperateIn.length > 0">
                                    <span class="operating-days-chips">
                                        {{  t('lineVersion.doesNotOperateIn') }}
                                        <v-chip
                                            v-for="day in op.doesNotOperateIn"
                                            :key="day.toISOString()"
                                            :text="d(day, 'short')"
                                            label
                                            density="compact"
                                        />
                                    </span>
                                </span>
                            </div>
                            <table>
                                <tbody>
                                    <tr>
                                        <template v-if="op.journeys.some((j) => Object.values(j.facilities).some((f) => f.active))">
                                            <td></td>
                                            <td class="header-column"></td>
                                            <td
                                                v-for="(journey, kdx) in op.journeys"
                                                :key="kdx"
                                                :class="{ 'journey-column': true, 'left-border': kdx !== 0, 'header-row': true }"
                                            >
                                                <span class="journey-facilities">
                                                    <template
                                                        v-for="(facility, key) in journey.facilities"
                                                        :key="key"
                                                    >
                                                        <FacilityIcon
                                                            v-if="facility.active"
                                                            :facility="facility"
                                                            compact
                                                        />
                                                    </template>
                                                </span>
                                            </td>
                                        </template>
                                    </tr>
                                    <tr
                                        v-for="(stopEntry, idx) in tab.displayStops"
                                        :key="idx"
                                    >
                                        <td class="icon-td">
                                            <div class="stop-facilities">
                                                <TransportBanIcons
                                                    :transport-ban-groups="stopEntry.banGroups"
                                                    compact
                                                />
                                                <template v-for="(facility, key) in stopEntry.facilities" :key="key">
                                                    <FacilityIcon
                                                        v-if="facility.active"
                                                        :facility="facility"
                                                        compact
                                                    />
                                                </template>
                                            </div>
                                        </td>
                                        <td class="header-column">{{ stopEntry.stopName }}</td>
                                        <td
                                            v-for="(journey, kdx) in op.journeys"
                                            :key="kdx"
                                            :class="{ 'journey-column': true, 'left-border': kdx !== 0 }"
                                        >
                                            {{ journey.schedule[idx] != null ? d(journey.schedule[idx]!.toDate(), 'timeShort') : 'x' }}
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </v-tabs-window-item>
                    </v-tabs-window>
                </v-tabs-window-item>
            </v-tabs-window>
        </div>
    </div>
</template>

<style scoped>
.wall-timetable {
    display: flex;
    flex-direction: column;
    min-height: 0;
    padding: 1em;
}

.timetable-header {
    flex: 0 0 auto;
    font-weight: bold;
    font-size: 1.15em;
}

.timetable-header-info {
    padding-inline-end: 2em;
    display: flex;
    gap: 1em;
}

.timetable-header-info > div {
    flex-grow: 1;
}

.timetable-header-dates {
    overflow: hidden;
    flex-shrink: 1;
    display: grid;
    grid-template-columns: max-content auto;
    grid-template-rows: auto auto;
    flex-wrap: nowrap;
    align-items: baseline;
    gap: 0 0.5em;
}

.timetable-header-chips {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    min-width: 0;
    gap: 0.25em;
}

.timetable-header-chips .v-chip {
    flex: 0 0 auto;
}

.wall-timetable-close-button {
    position: absolute;
    top: 0.25em;
    right: 0.25em;
}

.timetable-body-wrapper,
.timetable-body-wrapper :deep(.v-window),
.timetable-body-wrapper :deep(.v-window__container),
.timetable-body-wrapper :deep(.v-tabs-window-item),
.passing-times {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-height: 0;
}

.passing-times {
    overflow: auto;
}

.operating-days-chips {
    display: inline-flex;
    flex-wrap: nowrap;
    flex-direction: row;
    gap: 0.25em;
    white-space: nowrap;
    align-items: baseline;
}

.journey-facilities {
    display: flex;
    flex-direction: column;
    gap: 0.125em;
    align-items: center;
}

.stop-facilities {
    display: flex;
    gap: 0.125em;
    align-items: center;
}

table {
    border-collapse: separate;
    border-spacing: 0;
    margin-inline: auto;
}

tr:nth-child(even) td,
.selected-tab {
    background-color: color-mix(in srgb, black 10%, white);
}
tr:nth-child(odd) td {
    background-color: white;
}

td {
    padding-block: 0.125em;
    padding-inline: 0.5em;
    border: 0;
}

.icon-td {
    padding-inline-end: 0;
}

.journey-column {
    text-align: center;
}

.left-border {
    border-left: 1px solid currentColor;
}

.header-row {
    border-bottom: 1px solid currentColor;
    position: sticky;
    top: 0;
    z-index: 402;
}

.header-column {
    border-right: 1px solid currentColor;
    position: sticky;
    left: 0;
    z-index: 403;
}

.operation-time {
    font-size: 1.15em;
    font-weight: bold;
    display: flex;
    flex-direction: column;
    gap: 0.125em;
    margin-block: 0.5em;
}
</style>
