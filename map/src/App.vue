<script setup lang="ts">
import MapScreen from '@/map/MapScreen.vue'
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'

const ABOUT_PAGE = 'about'
const MAP_PAGE = 'map-app'

const { t } = useI18n()
const tab = ref(ABOUT_PAGE)
</script>

<template>
    <v-tabs
        v-model="tab"
        color="on-primary"
        slider-color="primary"
        inset
        class="app-tabs"
        elevation="0"
        bg-color="background"
    >
        <img src="/logo.svg" :alt="t('document.openDataLabLogo')" class="logo" />
        <v-tab :value="ABOUT_PAGE">{{ t('document.aboutPage') }}</v-tab>
        <v-tab :value="MAP_PAGE">{{ t('document.mapPage') }}</v-tab>
    </v-tabs>
    <v-tabs-window v-model="tab">
        <v-tabs-window-item :value="ABOUT_PAGE"></v-tabs-window-item>
        <v-tabs-window-item :value="MAP_PAGE">
            <MapScreen :is-visible="tab === MAP_PAGE" />
        </v-tabs-window-item>
    </v-tabs-window>
</template>

<style>
body {
    margin: 0;
}
.map-screen {
    width: 100vw;
    height: 100vh;
    position: fixed;
}
.logo {
    height: 100%;
    padding: 0.5rem;
}
.app-tabs {
    position: absolute;
    z-index: 1000;
    box-shadow: none !important;
    top: 0.25em;
    right: 0.25em;
}
</style>
