<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

withDefaults(
    defineProps<{
        transportBanGroups: number[] | null | undefined
        compact?: boolean | undefined
    }>(),
    {
        compact: false,
    },
)
</script>

<template>
    <v-tooltip
        v-for="banGroup in transportBanGroups"
        :key="banGroup"
        :text="t('stopFacilities.transportBan')"
    >
        <template #activator="tooltipProps">
            <span
                v-bind="tooltipProps.props"
                :class="{ 'facility-text-smalls': compact, 'facility-text': true }"
            >
                §<sub>{{ banGroup + 1 }}</sub>
            </span>
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
