<script setup lang="ts">
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import LineVersionLabel from '@/map/LineVersionLabel.vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import FacilityIcon from '@/map/FacilityIcon.vue'
import TransportBanIcons from '@/map/TransportBanIcons.vue'
import type { JourneyDirection } from '@/api/model/enums'
import { getDisplayWallTimetable } from '@/map/wallTimetable'

const { t, d } = useI18n()

const emit = defineEmits<{
    close: [],
    'journey-selected': [journeyId: number, routeId: number],
}>()
const props = defineProps<{
    wallTimetable: WallTimetableWithDates
}>()

const displayWallTimetable = computed(() => getDisplayWallTimetable(props.wallTimetable, t))
const activeDirectionTab = ref<JourneyDirection>(displayWallTimetable.value[0]!.direction)
const activeOperatingPeriodTab = ref<number>(0)
const hoveredColumnIndex = ref<number | null>(null)
const collapsed = ref(false)

function onJourneySelected(journeyId: number, routeId: number) {
    collapsed.value = true
    emit('journey-selected', journeyId, routeId)
}

watch(
    activeDirectionTab,
    () =>
        (activeOperatingPeriodTab.value =
            displayWallTimetable.value[0]!.displayOperatingPeriods[0]!.relationalId),
    { immediate: true },
)
const activeDisplayDirection = computed(
    () => displayWallTimetable.value.find((tab) => tab.direction === activeDirectionTab.value)!,
)

function handleColumnHover(columnIndex: number) {
    hoveredColumnIndex.value = columnIndex
}

function handleColumnLeave() {
    hoveredColumnIndex.value = null
}
</script>

<template>
    <div class="wall-timetable">
        <div
            v-show="collapsed"
            class="timetable-handle"
        >
            <v-btn
                icon="mdi-timetable"
                variant="text"
                :rounded="false"
                @click="collapsed = false"
            />
        </div>
        <div
            v-show="!collapsed"
            class="timetable"
        >
            <div class="timetable-header">
                <div class="timetable-header-info">
                    <div>
                        <LineVersionLabel
                            :transport-mode="wallTimetable.lineVersion.transportMode"
                            :is-detour="wallTimetable.lineVersion.detour"
                            :short-name="wallTimetable.lineVersion.shortName"
                            :public-code="wallTimetable.lineVersion.publicCode"
                        />
                        <div class="wall-timetable-buttons">
                            <v-btn
                                icon="mdi-arrow-collapse-left"
                                size="small"
                                variant="text"
                                @click="collapsed = true"
                            />
                            <v-btn
                                class="wall-timetable-close-button"
                                icon="mdi-close"
                                size="small"
                                variant="text"
                                @click="emit('close')"
                            />
                        </div>
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
                            <v-icon
                                v-if="wallTimetable.lineVersion.activePeriods.length === 0"
                                icon="mdi-cancel"
                            />
                        </span>
                    </div>
                </div>
                <v-divider class="mt-1" opacity="1" />
                <v-tabs v-if="displayWallTimetable.length > 1" v-model="activeDirectionTab" grow>
                    <v-tab
                        v-for="tab in displayWallTimetable"
                        :key="tab.direction"
                        :text="tab.text"
                        :value="tab.direction"
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
                        v-for="tab in displayWallTimetable"
                        :key="tab.direction"
                        :value="tab.direction"
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
                                        {{
                                            op.regularDays
                                                .map((day) => t(`daysOfWeek.short.${day}`))
                                                .join(', ')
                                        }}
                                    </span>
                                    <span v-if="op.alsoOperatesIn.length > 0">
                                        <span class="operating-days-chips">
                                            {{
                                                t(
                                                    op.regularDays.length > 0
                                                        ? 'lineVersion.alsoOperatesIn'
                                                        : 'lineVersion.onlyOperatesIn',
                                                )
                                            }}
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
                                            {{ t('lineVersion.doesNotOperateIn') }}
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
                                            <template
                                                v-if="
                                                    op.journeys.some((j) =>
                                                        Object.values(j.facilities).some(
                                                            (f) => f.active,
                                                        ),
                                                    )
                                                "
                                            >
                                                <td></td>
                                                <td class="header-column"></td>
                                                <td
                                                    v-for="(journey, kdx) in op.journeys"
                                                    :key="kdx"
                                                    :class="{
                                                        'journey-column': true,
                                                        'left-border': kdx !== 0,
                                                        'header-row': true,
                                                        'hovered-column': hoveredColumnIndex === kdx,
                                                    }"
                                                    @click="onJourneySelected(journey.relationalId, journey.routeId)"
                                                    @mouseenter="handleColumnHover(kdx)"
                                                    @mouseleave="handleColumnLeave"
                                                >
                                                    <div class="journey-facilities">
                                                        <template
                                                            v-for="(
                                                                facility, key
                                                            ) in journey.facilities"
                                                            :key="key"
                                                        >
                                                            <FacilityIcon
                                                                v-if="facility.active"
                                                                :facility="facility"
                                                                compact
                                                            />
                                                        </template>
                                                    </div>
                                                </td>
                                            </template>
                                        </tr>
                                        <tr v-for="(stopEntry, idx) in tab.displayStops" :key="idx">
                                            <td class="icon-td">
                                                <div class="stop-facilities">
                                                    <TransportBanIcons
                                                        :transport-ban-groups="stopEntry.banGroups"
                                                        compact
                                                    />
                                                    <template
                                                        v-for="(facility, key) in stopEntry.facilities"
                                                        :key="key"
                                                    >
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
                                                :class="{
                                                    'journey-column': true,
                                                    'left-border': kdx !== 0,
                                                    'hovered-column': hoveredColumnIndex === kdx,
                                                }"
                                                @click="onJourneySelected(journey.relationalId, journey.routeId)"
                                                @mouseenter="handleColumnHover(kdx)"
                                                @mouseleave="handleColumnLeave"
                                            >
                                                {{
                                                    journey.schedule[idx] != null
                                                        ? d(
                                                              journey.schedule[idx]!.toDate(),
                                                              'timeShort',
                                                          )
                                                        : 'x'
                                                }}
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
    </div>
</template>

<style scoped>
.wall-timetable{
    display: flex;
    flex-direction: column;
    min-height: 0;
}

.timetable {
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
    padding-inline-end: 4em;
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
.timetable-header-chips .v-icon {
    align-self: center;
}

.wall-timetable-buttons {
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
    flex-wrap: wrap;
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

.stop-facilities :deep(.v-icon) {
    margin-inline-start: 0.25em;
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

.hovered-column {
    background-color: color-mix(in srgb, black 8%, white) !important;
    cursor: pointer;
}
tr:nth-child(even) .hovered-column {
    background-color: color-mix(in srgb, black 18%, white) !important;
}

td {
    padding-block: 0.125em;
    padding-inline: 0.5em;
    border: 0;
}

.icon-td {
    padding-inline: 0;
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
    position: sticky;
    left: 0;
}
</style>
