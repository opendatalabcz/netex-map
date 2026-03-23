import type { Journey } from '@/api/model/journey'
import type { Page, PageRequest } from '@/api/model/page'
import type { JourneysOperatingInFrame } from '@/api/model/journeysOperatingInFrame'
import HttpRequestSender from '@/api/httpRequestSender'

const JOURNEY_URI = 'journey'

const JourneyApi = {
    getJourneyPage(page: PageRequest): Promise<Page<Journey> | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI], { ...page, latitudeFirst: true })
    },
    getJourneysOperatingInFrame(
        lonMin: number,
        latMin: number,
        lonMax: number,
        latMax: number,
        zoom: number,
        day: Date,
    ): Promise<JourneysOperatingInFrame | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, 'day', day.toISOString()], {
            lonMin: lonMin,
            latMin: latMin,
            lonMax: lonMax,
            latMax: latMax,
            zoom: zoom,
        })
    },
}

export default JourneyApi
