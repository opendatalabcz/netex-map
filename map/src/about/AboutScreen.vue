<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'

const { t } = useI18n()
const { mobile } = useDisplay()
const FIT_CVUT_URL = import.meta.env.FE_FIT_CVUT_URL
const ODL_URL = import.meta.env.FE_ODL_URL
const PROFINIT_URL = import.meta.env.FE_PROFINIT_URL
const GITHUB_REPO_URL = import.meta.env.FE_GITHUB_REPO_URL
const JRUTIL_URL = import.meta.env.FE_JRUTIL_URL
const PID_MAP_URL = import.meta.env.FE_PID_MAP_URL
const CZECH_NETEX_DATA_URL = import.meta.env.FE_CZECH_NETEX_DATA_URL
const JRUTIL_DATA_URL = import.meta.env.FE_JRUTIL_DATA_URL
const CZECH_OSM_DATA_URL = import.meta.env.FE_CZECH_OSM_DATA_URL
const OSM_URL = import.meta.env.FE_OSM_URL
const GRAPHHOPER_URL = import.meta.env.FE_GRAPHHOPER_URL
const AUTHOR_URL = import.meta.env.FE_AUTHOR_URL
</script>

<template>
    <div id="about" class="about-screen">
        <h1 :class="{ 'mt-13': mobile }">
            {{ t('aboutPage.title') }}
        </h1>
        <i18n-t keypath="aboutPage.introduction.p1" tag="p" scope="global">
            <template #fitCvut>
                <a :href="FIT_CVUT_URL">{{ t('aboutPage.introduction.fitCvut') }}</a>
            </template>
            <template #odl>
                <a :href="ODL_URL">{{ t('aboutPage.introduction.odl') }}</a>
            </template>
        </i18n-t>
        <i18n-t keypath="aboutPage.introduction.p2" tag="p" scope="global">
            <template #jrUtil>
                <a :href="JRUTIL_URL">{{ t('aboutPage.introduction.jrUtil') }}</a>
            </template>
        </i18n-t>

        <nav class="toc" aria-label="Table of contents">
            <h2>{{ t('aboutPage.tableOfContents') }}</h2>
            <ul>
                <li>
                    <a href="#app-features">{{ t('aboutPage.appFeatures.title') }}</a>
                </li>
                <li>
                    <a href="#manual">{{ t('aboutPage.manual.title') }}</a>
                    <ul>
                        <li><a href="#manual-cityLines">{{ t('aboutPage.manual.cityLines.title') }}</a></li>
                        <li><a href="#manual-search">{{ t('aboutPage.manual.search.title') }}</a></li>
                        <li><a href="#manual-journeyDetail">{{ t('aboutPage.manual.journeyDetail.title') }}</a></li>
                        <li><a href="#manual-timeControls">{{ t('aboutPage.manual.timeControls.title') }}</a></li>
                        <li><a href="#manual-openMap">{{ t('aboutPage.manual.openMap.title') }}</a></li>
                    </ul>
                </li>
                <li>
                    <a href="#data-sources">{{ t('aboutPage.dataSources.title') }}</a>
                </li>
                <li>
                    <a href="#methods">{{ t('aboutPage.methods.title') }}</a>
                </li>
            </ul>
        </nav>

        <h2 id="app-features">
            {{ t('aboutPage.appFeatures.title') }}
        </h2>
        <i18n-t keypath="aboutPage.appFeatures.p1" tag="p" scope="global">
            <template #pidMap>
                <a :href="PID_MAP_URL">{{ t('aboutPage.appFeatures.pidMap') }}</a>
            </template>
        </i18n-t>
        <figure>
            <figcaption>{{ t('aboutPage.appFeatures.ul') }}</figcaption>
            <ul>
                <li v-for="listItem in ['li1', 'li2', 'li3']" :key="listItem" class="mt-2">
                    <b>{{ t(`aboutPage.appFeatures.${listItem}.title`) }}:</b>
                    {{ t(`aboutPage.appFeatures.${listItem}.text`) }}
                </li>
            </ul>
        </figure>

        <h2 id="manual">
            {{ t('aboutPage.manual.title') }}
        </h2>
        <p>
            {{ t('aboutPage.manual.disclaimer') }}
        </p>
        <template
            v-for="manualEntry in [
                ['cityLines', 2],
                ['search', 4],
                ['journeyDetail', 2],
                ['timeControls', 4],
                ['openMap', 1],
            ]"
            :key="manualEntry[0]"
        >
            <h3 :id="`manual-${manualEntry[0]}`">
                {{ t(`aboutPage.manual.${manualEntry[0]}.title`) }}
            </h3>
            <ol>
                <li v-for="listItem in manualEntry[1]" :key="listItem" class="mt-2">
                    {{ t(`aboutPage.manual.${manualEntry[0]}.li${listItem}`) }}
                </li>
            </ol>
        </template>

        <h2 id="data-sources">
            {{ t('aboutPage.dataSources.title') }}
        </h2>
        <p>
            {{ t('aboutPage.dataSources.p1') }}
        </p>
        <p>
            {{ t('aboutPage.dataSources.p2') }}
        </p>
        <figure>
            <figcaption>{{ t('aboutPage.dataSources.ul') }}</figcaption>
            <ul>
                <li
                    v-for="listItem in [
                        ['li1', [['dataSource', CZECH_NETEX_DATA_URL]]],
                        [
                            'li2',
                            [
                                ['dataSource', JRUTIL_DATA_URL],
                                ['jrUtil', JRUTIL_URL],
                            ],
                        ],
                        [
                            'li3',
                            [
                                ['dataSource', CZECH_OSM_DATA_URL],
                                ['osm', OSM_URL],
                            ],
                        ],
                    ]"
                    :key="listItem[0] + ''"
                    class="mt-2"
                >
                    <b>{{ t(`aboutPage.dataSources.${listItem[0]}.title`) }}: </b>
                    <i18n-t :keypath="`aboutPage.dataSources.${listItem[0]}.text`" scope="global">
                        <template v-for="slotItem in listItem[1]" :key="slotItem[0]" #[slotItem[0]]>
                            <a :href="slotItem[1]" target="_blank">{{
                                t(`aboutPage.dataSources.${listItem[0]}.${slotItem[0]}`)
                            }}</a>
                        </template>
                    </i18n-t>
                </li>
            </ul>
        </figure>

        <h2 id="methods">
            {{ t('aboutPage.methods.title') }}
        </h2>
        <figure>
            <figcaption>{{ t('aboutPage.methods.ul') }}</figcaption>
            <ul>
                <li
                    v-for="listItem in [
                        ['li1', []],
                        ['li2', [['graphHopper', GRAPHHOPER_URL]]],
                        ['li3', []],
                        ['li4', []],
                        ['li5', []],
                    ]"
                    :key="listItem[0] + ''"
                    class="mt-2"
                >
                    <b>{{ t(`aboutPage.methods.${listItem[0]}.title`) }}: </b>
                    <i18n-t :keypath="`aboutPage.methods.${listItem[0]}.text`" scope="global">
                        <template v-for="slotItem in listItem[1]" :key="slotItem[0]" #[slotItem[0]]>
                            <a :href="slotItem[1]" target="_blank">{{
                                t(`aboutPage.methods.${listItem[0]}.${slotItem[0]}`)
                            }}</a>
                        </template>
                    </i18n-t>
                </li>
            </ul>
        </figure>

        <hr />
        <footer>
            <span>
                {{ t('footer.year') }}
                <a :href="AUTHOR_URL" target="_blank">
                    {{ t('footer.author') }}
                </a>.
                <i18n-t keypath="footer.fitCvutAttribution" scope="global">
                    <a :href="FIT_CVUT_URL" target="_blank">{{ t('footer.fitCvut') }}</a>
                </i18n-t>
            </span>
            <span>
                {{ t('footer.disclaimer') }}
            </span>
            <span class="logo-row">
                <a :href="ODL_URL" target="_blank">
                    <img
                        src="/odl-logo.svg"
                        :alt="t('footer.openDataLabLogo')"
                        class="logo"
                    />
                </a>
                <a :href="FIT_CVUT_URL" target="_blank">
                    <img src="/logo-fit-cs-modra.jpg" :alt="t('footer.fitCvutLogo')" class="logo" />
                </a>
                <a :href="PROFINIT_URL" target="_blank">
                    <img src="/logo-profinit.svg" :alt="t('footer.profinitLogo')" class="logo" />
                </a>
                <a :href="GITHUB_REPO_URL" target="_blank">
                    <img src="/logo-GitHub.svg" :alt="t('footer.gitHubLogo')" class="small-logo" />
                </a>
            </span>
        </footer>
    </div>
</template>

<style scoped>
footer {
    width: 100%;
    text-align: center;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 0.75em;
}
a {
    color: rgb(58, 118, 229);
    text-decoration: none;
}
a:hover {
    color: rgb(12, 74, 187);
}
figure {
    margin-inline: 0;
}
ul,
ol {
    margin-block-start: 0;
}
h2, h3 {
    margin-block-end: 0.5em;
}
.logo-row {
    display: inline-flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5em 2em;
}
.logo {
    height: 3rem;
}
.small-logo {
    height: 2.5rem;
}
</style>
