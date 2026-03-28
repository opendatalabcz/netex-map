import { createI18n } from 'vue-i18n'
import { cs, csPluralization, csDatetimeFormats, csNumberFormats } from '@/plugins/i18n/cs'

type MessageSchema = typeof cs

const I18n = createI18n<{ message: MessageSchema }, 'cs'>({
    legacy: false,
    locale: 'cs',
    messages: { cs },
    pluralRules: {
        cs: csPluralization,
    },
    datetimeFormats: {
        cs: csDatetimeFormats,
    },
    numberFormats: {
        cs: csNumberFormats,
    },
})

export default I18n
export type { MessageSchema }
