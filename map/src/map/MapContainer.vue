<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { MapController } from '@/services/mapController'
import JourneyDetails from '@/map/JourneyDetails.vue'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'
import WallTimetable from '@/map/WallTimetable.vue'
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import { useI18n } from 'vue-i18n'
import type { SearchLineVersionWithDates } from '@/api/model/searchLineVersions'
import SearchLineVersion from '@/map/SearchLineVersion.vue'
import { vIntersectionObserver } from '@vueuse/components'
import MomentControls from '@/map/MomentControls.vue'

const { t } = useI18n()

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

async function onLineVersionSearchUpdate(value: string | null) {
    if (value == null || value.length === 0) {
        lineVersionSearchResult.value = []
        return
    }
    controller.debouncedLineVersionSearch(value)
}
function onSearchSentinelVisible([entry]: IntersectionObserverEntry[]) {
    if (entry?.isIntersecting) {
        controller.extendLineVersionSearch()
    }
}

onMounted(async () => {
    if (!mapContainer.value) return
    map = L.map(mapContainer.value, { minZoom: 8, maxZoom: 18, zoomControl: false }).setView(
        [50.05, 14.5],
        11,
    )
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
        />
    </v-card>
    <div class="search overlay">
        <v-card class="search-card">
            <v-text-field
                v-model="lineSearch"
                variant="outlined"
                prepend-inner-icon="mdi-magnify"
                hide-details
                density="comfortable"
                clearable
                persistent-clear
                :placeholder="t('searchLine')"
                @update:model-value="onLineVersionSearchUpdate"
            />
        </v-card>
        <v-scroll-y-transition>
            <div v-show="lineVersionSearchResult.length > 0" class="search-line-versions-wrapper">
                <v-card
                    v-for="(lineVersion, idx) in lineVersionSearchResult"
                    :key="lineSearch ?? '' + lineVersion.relationalId"
                    class="search-line-version-card"
                    elevation="3"
                    @click="controller.onWallTimetableSelected(lineVersion.relationalId)"
                >
                    <div
                        v-if="idx === lineVersionSearchResult.length - 2"
                        v-intersection-observer="onSearchSentinelVisible"
                    />
                    <SearchLineVersion :line-version="lineVersion" />
                </v-card>
            </div>
        </v-scroll-y-transition>
    </div>
    <v-card v-if="wallTimetable" class="wall-timetable-card overlay">
        <WallTimetable
            :wall-timetable="wallTimetable"
            @close="controller.clearSelectedWallTimetable()"
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
.journey-details-card {
    top: 0.5em;
    right: 0.5em;
    max-width: 25em;
}
.search {
    display: flex;
    flex-direction: column;
    gap: 0.5em;
    top: 0.5em;
    left: 0.5em;
    width: 25em;
    max-height: calc(85vh - 1em);
}
.search-card {
    flex: 0 0 auto;
}
.search-line-versions-wrapper {
    display: flex;
    flex: 1 1 auto;
    min-height: 0;
    flex-direction: column;
    gap: 0.5em;
    width: 25em;
    overflow: auto;
    &::-webkit-scrollbar {
        display: none;
    }
    scrollbar-width: none;
    -ms-overflow-style: none;
}
.search-line-version-card {
    padding: 0.5em;
    flex: 0 0 auto;
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
