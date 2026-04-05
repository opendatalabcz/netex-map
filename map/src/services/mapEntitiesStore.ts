import type {
    MapJourney,
    MapLineVersion,
    MapRawRoute,
    MapRoute,
} from '@/api/model/journeysOperatingInFrame'
import type { PositionedMapJourneyWithDates } from '@/services/interpolatePositions'
import { toMapJourneyWithDates, toMapRoute } from '@/services/toDeserializedTypes'

type RenderedMapJourney = PositionedMapJourneyWithDates & {
    vehicleMarker: L.Marker | null
    color: string | null
}

type RenderedMapRoute = MapRoute & {
    featureGroup: L.FeatureGroup | null
    color: string | null
}

type StoreEntry = {
    journeys: Map<number, RenderedMapJourney>
    routes: Map<number, RenderedMapRoute>
}

function toRenderedJourney(journey: MapJourney): RenderedMapJourney {
    const res = toMapJourneyWithDates(journey) as RenderedMapJourney
    res.vehicleMarker = null
    res.color = null
    return res
}

function toRenderedRoute(route: MapRawRoute): RenderedMapRoute {
    const res = toMapRoute(route) as RenderedMapRoute
    res.featureGroup = null
    res.color = null
    return res
}

export class MapEntitiesStore {
    store: StoreEntry = {
        journeys: new Map(),
        routes: new Map(),
    }

    get journeys() {
        return this.store.journeys
    }
    get routes() {
        return this.store.routes
    }

    addRoutes(newRoutes: MapRawRoute[]) {
        for (const route of newRoutes) {
            if (this.store.routes.has(route.relationalId)) continue
            this.store.routes.set(route.relationalId, toRenderedRoute(route))
        }
    }

    addJourneys(journeys: MapJourney[]) {
        for (const journey of journeys) {
            if (this.store.journeys.has(journey.relationalId)) continue
            this.store.journeys.set(journey.relationalId, toRenderedJourney(journey))
        }
    }
}

export type { RenderedMapJourney, RenderedMapRoute }

export { toRenderedJourney, toRenderedRoute }
