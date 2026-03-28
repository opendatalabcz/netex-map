import JourneyApi from '@/api/journeyApi'
import type { MapEntitiesStore } from '@/services/mapEntitiesStore'

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
        this.mapEntriesStore.addLineVersions(frame.lineVersions)
        this.mapEntriesStore.addJourneys(frame.journeys)
    }
}
