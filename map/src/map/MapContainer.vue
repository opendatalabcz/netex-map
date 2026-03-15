<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import JourneyApi from '@/api/journeyApi'
import { renderVehicles } from '@/map/renderVehicles'

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)

onMounted(async () => {
    if (!mapContainer.value) return
    map.value = L.map(mapContainer.value).setView([49.9605, 14.9178], 9)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map.value)
    const moment = new Date('2025-11-15T05:17:00')
    const journeys = await JourneyApi.getJourneysOperatingInDay(moment)
    if (journeys) {
        renderVehicles(map.value, moment, journeys)
    }
})

onUnmounted(() => {
    if (map.value) {
        map.value.remove()
    }
})
</script>

<template>
    <div ref="mapContainer" class="map" />
</template>

<style scoped>
.map {
    position: absolute;
    width: 100%;
    height: 100%;
}
</style>
