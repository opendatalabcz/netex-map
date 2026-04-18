<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
const emit = defineEmits<{
    'update:model-value': [map: L.Map | null]
}>()

onMounted(async () => {
    if (!mapContainer.value) return
    map = L.map(mapContainer.value, {
        minZoom: 10,
        maxZoom: 18,
        zoomControl: false,
    }).setView([50.05, 14.5], 11)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map)
    emit('update:model-value', map)
})
onUnmounted(() => {
    map?.remove()
    map = null
    emit('update:model-value', map)
})
</script>

<template>
    <div ref="mapContainer" class="map-container" />
</template>
