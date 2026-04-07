<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { MapController } from '@/services/mapController'
import JourneyDetails from '@/map/JourneyDetails.vue'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null

const controller = new MapController(new Date('2025-11-15T16:17:00'))
const journeyDetails = ref<JourneyDetailsWithTimes | null>(null)

onMounted(async () => {
    if (!mapContainer.value) return
    map = L.map(mapContainer.value, { minZoom: 8, maxZoom: 19 }).setView([49.9, 15.5], 10)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map)
    controller.setMap(map)
    controller.addJourneyDetailsListener((details) => {
        journeyDetails.value = details
    })
})

onUnmounted(() => {
    map?.remove()
})
</script>

<template>
    <div ref="mapContainer" class="map" />
    <v-card v-if="journeyDetails">
        <JourneyDetails
            :journey-details="journeyDetails"
            @close="controller.clearJourneyDetails()"
            @stop-selected="i => controller.highlightJourneyDetailsStop(i)"
        />
    </v-card>
</template>

<style scoped>
.map {
    position: absolute;
    width: 100%;
    height: 100%;
}
.v-card {
    z-index: 400;
    position: absolute;
    top: 0.5em;
    right: 0.5em;
    max-width: 25em;
}
.journey-details {
    max-height: 90vh;
}
</style>
