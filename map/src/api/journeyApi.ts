import type { JourneysOperatingInFrame } from '@/api/model/journeysOperatingInFrame'
import HttpRequestSender from '@/api/httpRequestSender'
import type { JourneyDetails } from './model/journeyDetails'

const JOURNEY_URI = 'journey'
const FRAME_LOCATION = 'date-hour'
const DETAILS_LOCATION = 'details'

const JourneyApi = {
    async getJourneysOperatingInFrame(
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
        const data = {
            excludedJourneyIds: excludedJourneyIds.length > 0 ? excludedJourneyIds : [],
            excludedJourneyIdsFromPreviousDay:
                excludedJourneyIdsFromPreviousDay.length > 0
                    ? excludedJourneyIdsFromPreviousDay
                    : [],
            excludedRouteIds: excludedRouteIds.length > 0 ? excludedRouteIds : [],
        }
        const response = await HttpRequestSender.post(
            [JOURNEY_URI, FRAME_LOCATION, day.toISOString()],
            data,
            {
                lonMin: lonMin,
                latMin: latMin,
                lonMax: lonMax,
                latMax: latMax,
                zoom: zoom,
            },
        )
        if (!response) return response
        if (response.status !== 200) return null
        return response.json()
    },

    getJourneyDetails(journeyId: number): Promise<JourneyDetails | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI, journeyId.toString(), DETAILS_LOCATION], null)
    },
}

export default JourneyApi
