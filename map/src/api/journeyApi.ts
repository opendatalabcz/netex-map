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
        excludedJourneyIds: number[] = [],
        excludedJourneyIdsFromPreviousDay: number[] = [],
        excludedRouteIds: number[] = [],
    ): Promise<JourneysOperatingInFrame | null | undefined> {
        const options: Record<string, unknown> = {
            lonMin: lonMin,
            latMin: latMin,
            lonMax: lonMax,
            latMax: latMax,
            zoom: zoom,
        }
        if (excludedJourneyIds.length > 0) {
            options.nj = excludedJourneyIds
        }
        if (excludedJourneyIdsFromPreviousDay.length > 0) {
            options.njpd = excludedJourneyIdsFromPreviousDay
        }
        if (excludedRouteIds.length > 0) {
            options.nr = excludedRouteIds
        }
        return HttpRequestSender.get([JOURNEY_URI, FRAME_LOCATION, day.toISOString()], options)
    },

    getJourneyDetails(journeyId: number): Promise<JourneyDetails | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, journeyId.toString(), DETAILS_LOCATION], null)
    },
}

export default JourneyApi
