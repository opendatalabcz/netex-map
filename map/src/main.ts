import { createApp } from 'vue'
import App from '@/App.vue'
import Vuetify from '@/plugins/vuetify'
import I18n from '@/plugins/i18n/i18n'

const VueApp = createApp(App).use(I18n).use(Vuetify).mount('#app')

VueApp.$watch(
    () => I18n.global,
    (i18n) => {
        document.title = i18n.t('document.title')
    },
    { immediate: true },
)
