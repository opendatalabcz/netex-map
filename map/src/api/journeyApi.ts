import type { Journey } from "@/api/model/journey"
import type { Page, PageRequest } from "@/api/model/page"
import HttpRequestSender from "@/api/httpRequestSender"

const JOURNEY_URI = "/journey"

const JourneyApi = {
    getJourneyPage(page: PageRequest): Promise<Page<Journey> | null | undefined> {
        return HttpRequestSender.get([JOURNEY_URI], page)
    }
}

export default JourneyApi
