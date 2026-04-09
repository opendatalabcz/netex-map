import JourneyApi from '@/api/journeyApi'
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import type { MapEntitiesStore } from '@/services/mapEntitiesStore'
import LineVersionApi from '@/api/lineVersionApi'
import {
    toJourneyDetailsWithTimes,
    toSearchLineVersionWithDates,
    toWallTimetableWithDates,
} from './toDeserializedTypes'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'
import type { SearchLineVersionWithDates } from '@/api/model/searchLineVersions'
import type { Page } from '@/api/model/page'

export class MapEntitiesRetriever {
    mapEntriesStore: MapEntitiesStore

    constructor(mapEntriesStore: MapEntitiesStore) {
        this.mapEntriesStore = mapEntriesStore
    }

    async fetchFrame(bounds: L.LatLngBounds, zoom: number, moment: Date) {
        const excludedJourneyIds: number[] = []
        const excludedJourneyIdsFromPreviousDay: number[] = []
        for (const j of this.mapEntriesStore.journeys.values()) {
            if (j.fromPreviousDay) {
                excludedJourneyIdsFromPreviousDay.push(j.relationalId)
            } else {
                excludedJourneyIds.push(j.relationalId)
            }
        }
        const frame = await JourneyApi.getJourneysOperatingInFrame(
            bounds.getWest(),
            bounds.getSouth(),
            bounds.getEast(),
            bounds.getNorth(),
            zoom,
            moment,
            excludedJourneyIds,
            excludedJourneyIdsFromPreviousDay,
            Array.from(this.mapEntriesStore.routes.keys()),
        )
        if (!frame) return

        this.mapEntriesStore.addRoutes(frame.routes)
        this.mapEntriesStore.addJourneys(frame.journeys)
    }

    async fetchWallTimetable(
        lineVersionId: number,
    ): Promise<WallTimetableWithDates | null | undefined> {
        const timetable = await LineVersionApi.getLineVersionWallTimetable(lineVersionId)
        if (timetable == null) return null
        return toWallTimetableWithDates(timetable)
    }

    async fetchJourneyDetails(
        journeyId: number,
    ): Promise<JourneyDetailsWithTimes | null | undefined> {
        const details = await JourneyApi.getJourneyDetails(journeyId)
        if (details == null) return null
        return toJourneyDetailsWithTimes(details)
    }

    async searchLineVersions(
        query: string,
        pageSize?: number | undefined,
        pageNumber?: number | undefined,
    ): Promise<Page<SearchLineVersionWithDates> | null | undefined> {
        const result = await LineVersionApi.searchLineVersions(query, pageSize, pageNumber)
        if (result == null) return null
        return {
            page: result.page,
            content: result.content.map(toSearchLineVersionWithDates),
        }
    }
}
