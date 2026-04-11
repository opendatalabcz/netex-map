import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import '@mdi/font/css/materialdesignicons.css'
import { cs } from 'vuetify/locale'
import { VDateInput } from 'vuetify/labs/VDateInput'

type Theme = 'light' | 'dark'
const Themes: Record<string, Theme> = {
    light: 'light',
    dark: 'dark',
}

const Vuetify = createVuetify({
    locale: {
        locale: 'cs',
        messages: { cs },
    },
    icons: {
        defaultSet: 'mdi',
        aliases,
        sets: {
            mdi,
        },
    },
    display: {
        mobileBreakpoint: 'sm',
        thresholds: {
            xs: 0,
            sm: 670,
        },
    },
    theme: {
        defaultTheme: Themes.light,
    },
    components: {
        VDateInput,
    },
})

export default Vuetify
export type { Theme }
export { Themes }
