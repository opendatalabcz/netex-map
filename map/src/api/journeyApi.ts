import type { JourneysOperatingInFrame } from '@/api/model/journeysOperatingInFrame'
import HttpRequestSender from '@/api/httpRequestSender'

const JOURNEY_URI = 'journey'

const JourneyApi = {
    getJourneysOperatingInFrame(
        lonMin: number,
        latMin: number,
        lonMax: number,
        latMax: number,
        zoom: number,
        day: Date,
    ): Promise<JourneysOperatingInFrame | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, 'date-hour', day.toISOString()], {
            lonMin: lonMin,
            latMin: latMin,
            lonMax: lonMax,
            latMax: latMax,
            zoom: zoom,
        })
    },
}

export default JourneyApi
