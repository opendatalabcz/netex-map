<script setup lang="ts">
import type { SearchLineVersionWithDates } from '@/api/model/searchLineVersions'
import LineVersionSearchResult from '@/map/LineVersionSearchResult.vue'
import { vIntersectionObserver } from '@vueuse/components'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const search = defineModel<string | undefined>('search', { required: true })
defineProps<{
    searchResults: SearchLineVersionWithDates[]
}>()
const emit = defineEmits<{
    'extend-search': []
    'wall-timetable-selected': [lineVersionId: number]
}>()

function onSearchSentinelVisible([entry]: IntersectionObserverEntry[]) {
    if (entry?.isIntersecting) {
        emit('extend-search')
    }
}
</script>

<template>
    <div class="wall-timetable-search">
        <v-card class="search-card">
            <v-text-field
                v-model="search"
                variant="outlined"
                prepend-inner-icon="mdi-magnify"
                hide-details
                density="comfortable"
                clearable
                persistent-clear
                :placeholder="t('searchLine')"
            />
        </v-card>
        <v-scroll-y-transition>
            <div v-show="searchResults.length > 0" class="search-line-versions-wrapper">
                <v-card
                    v-for="(lineVersion, idx) in searchResults"
                    :key="search ?? '' + lineVersion.relationalId"
                    class="search-line-version-card"
                    elevation="3"
                    @click="emit('wall-timetable-selected', lineVersion.relationalId)"
                >
                    <div
                        v-if="idx === searchResults.length - 2"
                        v-intersection-observer="onSearchSentinelVisible"
                    />
                    <LineVersionSearchResult :line-version="lineVersion" />
                </v-card>
            </div>
        </v-scroll-y-transition>
    </div>
</template>

<style scoped>
.wall-timetable-search {
    display: flex;
    flex-direction: column;
    gap: 0.5em;
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
</style>
