<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { MapEntitiesRenderer } from '@/services/mapEntitiesRenderer'
import { MapEntitiesStore } from '@/services/mapEntitiesStore'
import { debounce } from '@/util/debounce'
import { MapEntitiesRetriever } from '@/services/mapEntitiesRetriever'

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let mapEntitiesRenderer: MapEntitiesRenderer | null = null
const mapEntitiesStore = new MapEntitiesStore()
const mapEntitiesRetriever = new MapEntitiesRetriever(mapEntitiesStore)

const moment = new Date('2025-11-15T05:17:00')
const debounceUpdate = debounce(async () => {
    if (!mapEntitiesRenderer || !map) return
    await mapEntitiesRetriever.fetchFrame(map.getBounds(), map.getZoom(), moment)
    mapEntitiesRenderer.renderFrame(moment)
}, 400)

onMounted(async () => {
    if (!mapContainer.value) return
    map = L.map(mapContainer.value, { minZoom: 8, maxZoom: 19 }).setView([49.9, 15.5], 10)
    mapEntitiesRenderer = new MapEntitiesRenderer(map, mapEntitiesStore)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map)
    await mapEntitiesRetriever.fetchFrame(map.getBounds(), map.getZoom(), moment)
    mapEntitiesRenderer.renderFrame(moment)
    map.on('move', debounceUpdate)
    map.on('zoom', debounceUpdate)
})

onUnmounted(() => {
    map?.remove()
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
