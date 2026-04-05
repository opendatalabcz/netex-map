import type { JourneysOperatingInFrame } from '@/api/model/journeysOperatingInFrame'
import HttpRequestSender from '@/api/httpRequestSender'
import type { JourneyDetails } from './model/journeyDetails'

const JOURNEY_URI = 'journey'
const FRAME_LOCATION = 'date-hour'
const DETAILS_LOCATION = 'details'

const JourneyApi = {
    getJourneysOperatingInFrame(
        lonMin: number,
        latMin: number,
        lonMax: number,
        latMax: number,
        zoom: number,
        day: Date,
    ): Promise<JourneysOperatingInFrame | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, FRAME_LOCATION, day.toISOString()], {
            lonMin: lonMin,
            latMin: latMin,
            lonMax: lonMax,
            latMax: latMax,
            zoom: zoom,
        })
    },

    getJourneyDetails(journeyId: number): Promise<JourneyDetails | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, journeyId.toString(), DETAILS_LOCATION], null)
    },
}

export default JourneyApi
