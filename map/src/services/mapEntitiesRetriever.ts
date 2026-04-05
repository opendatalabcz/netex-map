import JourneyApi from '@/api/journeyApi'
import type { JourneyWithDatesAndTimes } from '@/api/model/journey'
import type { MapEntitiesStore } from '@/services/mapEntitiesStore'
import { toJourneyWithDatesAndTimes } from './toDeserializedTypes'

export class MapEntitiesRetriever {
    mapEntriesStore: MapEntitiesStore

    constructor(mapEntriesStore: MapEntitiesStore) {
        this.mapEntriesStore = mapEntriesStore
    }

    async fetchFrame(bounds: L.LatLngBounds, zoom: number, moment: Date) {
        const frame = await JourneyApi.getJourneysOperatingInFrame(
            bounds.getWest(),
            bounds.getSouth(),
            bounds.getEast(),
            bounds.getNorth(),
            zoom,
            moment,
        )
        if (!frame) return

        this.mapEntriesStore.addRoutes(frame.routes)
        this.mapEntriesStore.addJourneys(frame.journeys)
    }

    async fetchJourney(journeyId: number): Promise<JourneyWithDatesAndTimes | null> {
        const journey = await JourneyApi.getJourneyById(journeyId)
        if (journey == null) return null
        return toJourneyWithDatesAndTimes(journey)
    }
}
