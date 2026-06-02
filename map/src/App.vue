<script setup lang="ts">
import MapScreen from '@/map/MapScreen.vue'
import AboutScreen from '@/about/AboutScreen.vue'
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
        class="app-tabs elevation-1"
        bg-color="background"
    >
        <img
            v-show="tab === MAP_PAGE"
            src="/odl-logo.svg"
            :alt="t('footer.openDataLabLogo')"
            class="cards-logo"
        />
        <v-tab :value="ABOUT_PAGE">{{ t('document.aboutPage') }}</v-tab>
        <v-tab :value="MAP_PAGE">{{ t('document.mapPage') }}</v-tab>
    </v-tabs>
    <v-tabs-window v-model="tab">
        <v-tabs-window-item :value="ABOUT_PAGE">
            <AboutScreen />
        </v-tabs-window-item>
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
.about-screen {
    padding: 1em;
    max-width: 60em;
    margin: auto;
}
.cards-logo {
    height: 100%;
    padding: 0.5rem;
}
.app-tabs {
    position: fixed;
    z-index: 1000;
    top: 0.25em;
    right: 0.25em;
}
</style>
