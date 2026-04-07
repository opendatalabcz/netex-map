import JourneyApi from '@/api/journeyApi'
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import type { MapEntitiesStore } from '@/services/mapEntitiesStore'
import LineVersionApi from '@/api/lineVersionApi'
import { toJourneyDetailsWithTimes, toWallTimetableWithDates } from './toDeserializedTypes'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'

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
}
