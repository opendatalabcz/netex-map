import { createApp } from 'vue'
import App from '@/App.vue'
import Vuetify from '@/plugins/vuetify'
import I18n from '@/plugins/i18n/i18n'
import 'unfonts.css'

createApp(App).use(I18n).use(Vuetify).mount('#app')
