<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)

onMounted(() => {
    if (!mapContainer.value) return
    map.value = L.map(mapContainer.value).setView([49.9605, 14.9178], 9)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map.value)
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
