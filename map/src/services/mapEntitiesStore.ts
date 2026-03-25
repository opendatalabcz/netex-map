import type {
    MapJourney,
    MapLineVersion,
    MapRawRoute,
    MapRoute,
} from '@/api/model/journeysOperatingInFrame'
import type { PositionedMapJourneyWithDates } from '@/services/interpolatePositions'
import { toMapRoute, toMapScheduledStopWithDates } from '@/services/toDeserializedTypes'

type RenderedMapJourney = PositionedMapJourneyWithDates & {
    vehicleMarker: L.Marker | null
}

type StoreEntry = {
    journeys: Map<number, RenderedMapJourney>
    routes: Map<number, MapRoute>
    lineVersions: Map<number, MapLineVersion>
}

function toRenderedJourney(journey: MapJourney): RenderedMapJourney {
    return {
        relationalId: journey.relationalId,
        lineVersionId: journey.lineVersionId,
        routeId: journey.routeId,
        schedule: journey.schedule.map(toMapScheduledStopWithDates),
        nextDayFirstStopIndex: journey.nextDayFirstStopIndex,
        fromPreviousDay: journey.fromPreviousDay,
        position: null,
        segmentIndex: null,
        azimuth: null,
        vehicleMarker: null,
    }
}

export class MapEntitiesStore {
    store: StoreEntry = {
        journeys: new Map(),
        routes: new Map(),
        lineVersions: new Map(),
    }

    get journeys() {
        return this.store.journeys
    }
    get routes() {
        return this.store.routes
    }
    get lineVersions() {
        return this.store.lineVersions
    }

    addRoutes(newRoutes: MapRawRoute[]) {
        for (const route of newRoutes) {
            if (this.store.routes.has(route.relationalId)) continue
            this.store.routes.set(route.relationalId, toMapRoute(route))
        }
    }

    addLineVersions(lineVersions: MapLineVersion[]) {
        for (const lineVersion of lineVersions) {
            if (this.store.lineVersions.has(lineVersion.relationalId)) continue
            this.store.lineVersions.set(lineVersion.relationalId, lineVersion)
        }
    }

    addJourneys(journeys: MapJourney[]) {
        for (const journey of journeys) {
            if (this.store.journeys.has(journey.relationalId)) continue
            this.store.journeys.set(journey.relationalId, toRenderedJourney(journey))
        }
    }
}

export type { RenderedMapJourney }

export { toRenderedJourney }
