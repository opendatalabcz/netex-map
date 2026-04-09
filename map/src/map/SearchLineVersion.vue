<script setup lang="ts">
import type { TransportMode } from '@/api/model/enums'
import type { WallActivePeriodWithDates, WallOperator } from '@/api/model/wallTimetable'
import LineVersionLabel from '@/map/LineVersionLabel.vue'
import { useI18n } from 'vue-i18n'

const { t, d } = useI18n()

defineProps<{
    lineVersion: {
        transportMode: TransportMode
        detour: boolean
        shortName: string
        publicCode: string
        operator: WallOperator
        validFrom: Date
        validTo: Date
        activePeriods: WallActivePeriodWithDates[]
    }
}>()
</script>

<template>
    <div class="search-line-version">
        <LineVersionLabel
            :transport-mode="lineVersion.transportMode"
            :is-detour="lineVersion.detour"
            :short-name="lineVersion.shortName"
            :public-code="lineVersion.publicCode"
        />
        {{ lineVersion.operator.legalName }}
        <v-divider opacity="1" />
        <div class="header-dates">
            {{ t('lineVersion.validIn') }}
            <span class="header-chips">
                <v-chip
                    :text="`${d(lineVersion.validFrom, 'short')} - ${d(lineVersion.validTo, 'short')}`"
                    label
                    density="compact"
                />
            </span>
            {{ t('lineVersion.activeIn') }}
            <span class="header-chips">
                <v-chip
                    v-for="ap in lineVersion.activePeriods"
                    :key="ap.fromDate.toISOString()"
                    :text="`${d(ap.fromDate, 'short')} - ${d(ap.toDate, 'short')}`"
                    label
                    density="compact"
                />
                <v-icon v-if="lineVersion.activePeriods.length === 0" icon="mdi-cancel" />
            </span>
        </div>
    </div>
</template>

<style scoped>
.search-line-version {
    display: flex;
    flex-direction: column;
    gap: 0.125em;
}

.search-line-version > div {
    flex-grow: 1;
}

.header-dates {
    overflow: hidden;
    flex-shrink: 1;
    display: grid;
    grid-template-columns: max-content auto;
    grid-template-rows: auto auto;
    flex-wrap: nowrap;
    align-items: baseline;
    gap: 0.125em 0.5em;
}

.header-chips {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    min-width: 0;
    gap: 0.25em;
}

.header-chips .v-chip {
    flex: 0 0 auto;
}
.header-chips .v-icon {
    align-self: center;
}
</style>
