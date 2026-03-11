import type { Journey } from '@/api/model/journey'
import type { Page, PageRequest } from '@/api/model/page'
import type { JourneysOperatingInDay } from '@/api/model/journeysOperatingInDay'
import HttpRequestSender from '@/api/httpRequestSender'

const JOURNEY_URI = 'journey'

const JourneyApi = {
    getJourneyPage(page: PageRequest): Promise<Page<Journey> | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI], { ...page, latitudeFirst: true })
    },
    getJourneysOperatingInDay(day: Date): Promise<JourneysOperatingInDay | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, 'day', day.toISOString().split('T')[0]!], {
            latitudeFirst: true,
            timezone: 'Europe/Prague',
        })
    },
}

export default JourneyApi
