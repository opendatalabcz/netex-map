<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { renderFrame } from '@/map/render'

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)

const debounce = (callback: (...args: unknown[]) => void, wait: number) => {
    let timeoutId: number | undefined = undefined;
    return (...args: unknown[]) => {
        window.clearTimeout(timeoutId);
        timeoutId = window.setTimeout(() => {
            callback(...args);
        }, wait);
    };
}

const moment = new Date('2025-11-15T05:17:00')
const debounceUpdate = debounce(() => {
    if (!map.value) return
    renderFrame(map.value, moment)
}, 400)

onMounted(async () => {
    if (!mapContainer.value) return
    map.value = L.map(mapContainer.value, { minZoom: 8, maxZoom: 19 }).setView([49.9, 15.5], 11)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map.value)
    renderFrame(map.value, moment)
    map.value.on('move', debounceUpdate)
    map.value.on('zoom', debounceUpdate)
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
