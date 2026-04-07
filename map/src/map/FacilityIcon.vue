<script setup lang="ts">
import type { DisplayFacility } from '@/map/facilities'

withDefaults(
    defineProps<{
        facility: DisplayFacility
        compact?: boolean | undefined
    }>(),
    {
        compact: false,
    },
)
</script>

<template>
    <v-tooltip :text="facility.tooltip">
        <template #activator="tooltipProps">
            <span
                v-if="facility.text"
                v-bind="tooltipProps.props"
                :class="{ 'facility-text-smalls': compact, 'facility-text': true }"
            >
                {{ facility.text }}
            </span>
            <v-icon
                v-if="facility.icon"
                :size="compact ? 'small' : 'default'"
                v-bind="{ ...tooltipProps.props, ...facility.iconProps }"
                :icon="facility.icon"
            />
        </template>
    </v-tooltip>
</template>

<style scoped>
.facility-text {
    font-weight: bold;
}
.facility-text-small {
    line-height: 1em;
    font-size: 0.9em;
}
</style>
