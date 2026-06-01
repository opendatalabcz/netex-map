import { createApp } from 'vue'
import App from '@/App.vue'
import Vuetify from '@/plugins/vuetify'
import I18n from '@/plugins/i18n/i18n'
import { useI18n } from 'vue-i18n'

const VueApp = createApp(App).use(I18n).use(Vuetify).mount('#app')

VueApp.$watch(() => useI18n().t, newVal => {
    document.title = newVal('document.title')
}, { immediate: true })
