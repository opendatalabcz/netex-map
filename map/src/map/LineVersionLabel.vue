<script setup lang="ts">
import type { TransportMode } from '@/api/model/enums'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const props = defineProps<{
    transportMode: TransportMode
    isDetour: boolean
    shortName: string
    publicCode: string
}>()

const transportMode = computed(() => {
    switch (props.transportMode) {
        case 'BUS':
            return {
                icon: 'mdi-bus',
                text: t('transportModes.bus'),
            }
        case 'TROLLEY_BUS':
            return {
                icon: 'mdi-bus',
                text: t('transportModes.trolleybus'),
            }
        case 'RAIL':
            return {
                icon: 'mdi-train',
                text: t('transportModes.rail'),
            }
        case 'FUNICULAR':
            return {
                icon: 'mdi-gondola',
                text: t('transportModes.funicular'),
            }
        case 'TRAM':
            return {
                icon: 'mdi-tram',
                text: t('transportModes.tram'),
            }
        case 'METRO':
            return {
                icon: 'mdi-subway-variant',
                text: t('transportModes.metro'),
            }
        default:
            return {
                icon: 'mdi-bus',
                text: t('transportModes.bus'),
            }
    }
})
</script>

<template>
    <v-tooltip v-if="isDetour" :text="t('lineVersion.detour')">
        <template #activator="tooltipProps">
            <v-icon v-bind="tooltipProps.props" icon="mdi-alert" size="small" color="warning" />
        </template>
    </v-tooltip>
    <v-icon :icon="transportMode.icon" />
    {{ transportMode.text }}
    {{ shortName }}
    <span v-if="shortName !== publicCode" class="public-code">
        {{ publicCode }}
    </span>
</template>

<style scoped>
.public-code {
    font-size: 0.75em;
    opacity: 0.6;
}
</style>
