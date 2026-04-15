import type { MapJourney, MapRawRoute, MapRoute } from '@/api/model/journeysOperatingInFrame'
import type { PositionedMapJourneyWithDates } from '@/services/interpolatePositions'
import { toMapJourneyWithDates, toMapRoute } from '@/services/toDeserializedTypes'

type RenderedMapJourney = PositionedMapJourneyWithDates & {
    vehicleMarker: L.Marker | null
    color: string | null
}

type RenderedMapRoute = MapRoute & {
    featureGroup: L.FeatureGroup | null
    stops: L.CircleMarker[][] | null
    color: string | null
}

type StoreEntry = {
    journeys: Map<number, Map<string, RenderedMapJourney>>
    routes: Map<number, RenderedMapRoute>
}

function toRenderedJourney(journey: MapJourney): RenderedMapJourney {
    const res: RenderedMapJourney = {
        ...toMapJourneyWithDates(journey),
        vehicleMarker: null,
        color: null,
        position: null,
        segmentIndex: null,
        azimuth: null,
    }
    return res
}

function toRenderedRoute(route: MapRawRoute): RenderedMapRoute {
    const res: RenderedMapRoute = {
        ...toMapRoute(route),
        featureGroup: null,
        stops: null,
        color: null,
    }
    return res
}

export class MapEntitiesStore {
    store: StoreEntry = {
        journeys: new Map(),
        routes: new Map(),
    }

    getMomentKeyFor(moment: Date): number {
        const normalizedMoment = new Date(moment)
        normalizedMoment.setMinutes(0, 0, 0)
        return normalizedMoment.getTime()
    }

    journeysForMoment(moment: Date) {
        return this.store.journeys.get(this.getMomentKeyFor(moment))?.values()
    }
    journeysForMomentKey(momentKey: number) {
        return this.store.journeys.get(momentKey)?.values()
    }

    routes() {
        return this.store.routes
    }

    addRoutes(newRoutes: MapRawRoute[]) {
        for (const route of newRoutes) {
            if (this.store.routes.has(route.relationalId)) continue
            this.store.routes.set(route.relationalId, toRenderedRoute(route))
        }
    }

    addJourneys(moment: Date, journeys: MapJourney[]) {
        const momentKey = this.getMomentKeyFor(moment)
        let timePartition = this.store.journeys.get(momentKey)
        if (timePartition == null) {
            timePartition = new Map()
            this.store.journeys.set(momentKey, timePartition)
        }
        for (const journey of journeys) {
            const key = '' + journey.relationalId + journey.fromPreviousDay
            if (timePartition.has(key)) continue
            timePartition.set(key, toRenderedJourney(journey))
        }
    }

    removeJourneysForMoment(momentKey: number) {
        this.store.journeys.delete(momentKey)
    }

    removeUnusedRoutes() {
        const usedRouteIds = new Set<number>()
        for (const [, journeyMap] of this.store.journeys) {
            for (const [, journey] of journeyMap) {
                usedRouteIds.add(journey.routeId)
            }
        }
        const routeIdsToDelete: number[] = []
        for (const [id] of this.store.routes) {
            if (!usedRouteIds.has(id)) routeIdsToDelete.push(id)
        }
        for (const id of routeIdsToDelete) {
            this.store.routes.delete(id)
        }
    }
}

export type { RenderedMapJourney, RenderedMapRoute }

export { toRenderedJourney, toRenderedRoute }
