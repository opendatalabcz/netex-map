<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { MapController } from '@/services/mapController'
import JourneyDetails from '@/map/JourneyDetails.vue'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'
import WallTimetable from '@/map/WallTimetable.vue'
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import type { SearchLineVersionWithDates } from '@/api/model/searchLineVersions'
import MomentControls from '@/map/MomentControls.vue'
import WallTimetableSearch from '@/map/WallTimetableSearch.vue'

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null

const moment = ref<Date>(new Date())
const playing = ref(false)
const playSpeed = ref(1)
const controller = new MapController()
const journeyDetails = ref<JourneyDetailsWithTimes | null>(null)
const wallTimetable = ref<WallTimetableWithDates | null>(null)
const lineSearch = ref<string | undefined>(undefined)
const lineVersionSearchResult = ref<SearchLineVersionWithDates[]>([])
const showSearch = computed(() => wallTimetable.value == null)
const wallTimetableCollapsed = ref(false)

function onJourneyDetailsUpdate(details: JourneyDetailsWithTimes | null) {
    journeyDetails.value = details
}
function onWallTimetableUpdate(timetable: WallTimetableWithDates | null) {
    wallTimetable.value = timetable
}
function onLineVersionSearchResultUpdate(lineVersions: SearchLineVersionWithDates[] | null) {
    lineVersionSearchResult.value = lineVersions ? [...lineVersions] : []
}
function onMomentUpdate(newMoment: Date) {
    moment.value = newMoment
}
function onAnimationPlayingUpdate(animationPlaying: boolean) {
    playing.value = animationPlaying
}
function onAnimationSpeedUpdate(speed: number) {
    playSpeed.value = speed
}

function onLineVersionSearchUpdate(value: string | undefined) {
    if (value == undefined || value.length === 0) {
        lineVersionSearchResult.value = []
        return
    }
    controller.debouncedLineVersionSearch(value)
}

function onShowTimetableThroughJourneyDetails() {
    if (journeyDetails.value == null) return
    wallTimetableCollapsed.value = false
    if (
        journeyDetails.value.lineVersion.relationalId ===
        wallTimetable.value?.lineVersion.relationalId
    ) {
        return
    }
    controller.onWallTimetableSelected(journeyDetails.value.lineVersion.relationalId)
}

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
    controller.setMap(map)
    controller.addJourneyDetailsListener(onJourneyDetailsUpdate)
    controller.addWallTimetableListener(onWallTimetableUpdate)
    controller.addLineVersionSearchListener(onLineVersionSearchResultUpdate)
    controller.addMomentListener(onMomentUpdate)
    controller.addAnimationPlayingListener(onAnimationPlayingUpdate)
    controller.addAnimationSpeedListener(onAnimationSpeedUpdate)
})

onUnmounted(() => {
    controller.setMap(null)
    controller.removeJourneyDetailsListener(onJourneyDetailsUpdate)
    controller.removeWallTimetableListener(onWallTimetableUpdate)
    controller.removeLineVersionSearchListener(onLineVersionSearchResultUpdate)
    controller.removeMomentListener(onMomentUpdate)
    controller.removeAnimationPlayingListener(onAnimationPlayingUpdate)
    controller.removeAnimationSpeedListener(onAnimationSpeedUpdate)
    map?.remove()
})
</script>

<template>
    <div ref="mapContainer" class="map" />
    <v-card v-if="journeyDetails" class="journey-details-card overlay">
        <JourneyDetails
            :journey-details="journeyDetails"
            @close="controller.clearJourneyDetails()"
            @stop-selected="(i) => controller.highlightJourneyDetailsStop(i)"
            @show-timetable="onShowTimetableThroughJourneyDetails"
        />
    </v-card>
    <WallTimetableSearch
        v-show="showSearch"
        v-model:search="lineSearch"
        :search-results="lineVersionSearchResult"
        class="overlay"
        @update:search="onLineVersionSearchUpdate"
        @extend-search="controller.extendLineVersionSearch()"
        @wall-timetable-selected="(l) => controller.onWallTimetableSelected(l)"
    />
    <v-card v-if="wallTimetable" class="wall-timetable-card overlay">
        <WallTimetable
            v-model:collapsed="wallTimetableCollapsed"
            :wall-timetable="wallTimetable"
            @close="controller.clearSelectedWallTimetable()"
            @journey-selected="(j, r) => controller.onWallJourneySelected(j, r)"
        />
    </v-card>
    <MomentControls
        :model-value="moment"
        :playing="playing"
        :play-speed="playSpeed"
        class="overlay"
        @update:model-value="(m) => controller.setMoment(m)"
        @update:playing="(p) => controller.setAnimationPlaying(p)"
        @update:play-speed="(s) => controller.setAnimationSpeed(s)"
    />
</template>

<style scoped>
.map {
    position: absolute;
    width: 100%;
    height: 100%;
}
.overlay {
    z-index: 400;
    position: absolute;
}
.wall-timetable-search {
    top: 0.5em;
    left: 0.5em;
    width: 25em;
    max-height: calc(85vh - 1em);
}
.journey-details-card {
    top: 0.5em;
    right: 0.5em;
    max-width: 25em;
}
.wall-timetable-card {
    top: 0.5em;
    left: 0.5em;
    max-width: calc(100% - 1em);
    z-index: 401;
}
.journey-details,
.wall-timetable {
    max-height: 90vh;
}
.moment-controls {
    left: 0.5em;
    bottom: 0.5em;
}
</style>
