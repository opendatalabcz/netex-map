<script setup lang="ts">
import MapContainer from '@/map/MapContainer.vue'
import JourneyDetails from '@/map/JourneyDetails.vue'
import WallTimetable from '@/map/WallTimetable.vue'
import MomentControls from '@/map/MomentControls.vue'
import WallTimetableSearch from '@/map/WallTimetableSearch.vue'
import SnackBar from '@/map/SnackBar.vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import type { SearchLineVersionWithDates } from '@/api/model/searchLineVersions'
import { AppController } from '@/services/appController'
import ThePopUpMessageController from '@/services/popUpMessageController'

const moment = ref<Date>(new Date())
const playing = ref(false)
const playSpeed = ref(1)
const popUpMessageController = ThePopUpMessageController
const appController = new AppController(popUpMessageController)
const journeyDetails = ref<JourneyDetailsWithTimes | null>(null)
const wallTimetable = ref<WallTimetableWithDates | null>(null)
const lineSearch = ref<string | undefined>(undefined)
const lineVersionSearchResult = ref<SearchLineVersionWithDates[]>([])
const showSearch = computed(() => wallTimetable.value == null)
const wallTimetableCollapsed = ref(false)

function onLineVersionSearchUpdate(value: string | undefined) {
    if (value == undefined || value.length === 0) {
        lineVersionSearchResult.value = []
        return
    }
    appController.debouncedLineVersionSearch(value)
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
    appController.onWallTimetableSelected(journeyDetails.value.lineVersion.relationalId)
}

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

onMounted(() => {
    appController.addJourneyDetailsListener(onJourneyDetailsUpdate)
    appController.addWallTimetableListener(onWallTimetableUpdate)
    appController.addLineVersionSearchListener(onLineVersionSearchResultUpdate)
    appController.addMomentListener(onMomentUpdate)
    appController.addAnimationPlayingListener(onAnimationPlayingUpdate)
    appController.addAnimationSpeedListener(onAnimationSpeedUpdate)
})
onUnmounted(() => {
    appController.removeJourneyDetailsListener(onJourneyDetailsUpdate)
    appController.removeWallTimetableListener(onWallTimetableUpdate)
    appController.removeLineVersionSearchListener(onLineVersionSearchResultUpdate)
    appController.removeMomentListener(onMomentUpdate)
    appController.removeAnimationPlayingListener(onAnimationPlayingUpdate)
    appController.removeAnimationSpeedListener(onAnimationSpeedUpdate)
})
</script>

<template>
    <MapContainer @update:model-value="(m) => appController.setMap(m)" />
    <v-card v-if="journeyDetails" class="journey-details-card overlay">
        <JourneyDetails
            :journey-details="journeyDetails"
            @close="appController.clearJourneyDetails()"
            @stop-selected="(i) => appController.highlightJourneyDetailsStop(i)"
            @show-timetable="onShowTimetableThroughJourneyDetails"
        />
    </v-card>
    <WallTimetableSearch
        v-show="showSearch"
        v-model:search="lineSearch"
        :search-results="lineVersionSearchResult"
        class="overlay"
        @update:search="onLineVersionSearchUpdate"
        @extend-search="appController.extendLineVersionSearch()"
        @wall-timetable-selected="(l) => appController.onWallTimetableSelected(l)"
    />
    <v-card v-if="wallTimetable" class="wall-timetable-card overlay">
        <WallTimetable
            v-model:collapsed="wallTimetableCollapsed"
            :wall-timetable="wallTimetable"
            @close="appController.clearSelectedWallTimetable()"
            @journey-selected="(j, r) => appController.onWallJourneySelected(j, r)"
        />
    </v-card>
    <MomentControls
        :model-value="moment"
        :playing="playing"
        :play-speed="playSpeed"
        class="overlay"
        @update:model-value="(m) => appController.setMoment(m)"
        @update:playing="(p) => appController.setAnimationPlaying(p)"
        @update:play-speed="(s) => appController.setAnimationSpeed(s)"
    />
    <SnackBar :controller="popUpMessageController" />
</template>

<style>
body {
    margin: 0;
}
</style>

<style scoped>
.map-container {
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
