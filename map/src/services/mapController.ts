import { MapEntitiesRenderer } from '@/services/mapEntitiesRenderer'
import { MapEntitiesRetriever } from '@/services/mapEntitiesRetriever'
import {
    MapEntitiesStore,
    type RenderedMapJourney,
    type RenderedMapRoute,
} from '@/services/mapEntitiesStore'
import { recalculateVehiclePosition } from '@/services/interpolatePositions'
import { debounce } from '@/util/debounce'
import type { JourneyWithDatesAndTimes } from '@/api/model/journey'

type FocusedJourney = {
    journeyId: number
    journey: JourneyWithDatesAndTimes | null
    mapRoute: RenderedMapRoute
}

export class MapController {
    private store: MapEntitiesStore
    private retriever: MapEntitiesRetriever
    private map: L.Map | null
    private renderer: MapEntitiesRenderer | null
    private moment: Date
    private momentListeners: ((moment: Date) => void)[] = []
    private focusedJourney: FocusedJourney | null = null
    private focusedJourneyListeners: ((focused: JourneyWithDatesAndTimes | null) => void)[] = []

    constructor(
        initialMoment: Date,
        store: MapEntitiesStore | null = null,
        map: L.Map | null = null,
    ) {
        this.store = store ?? new MapEntitiesStore()
        this.retriever = new MapEntitiesRetriever(this.store)
        this.map = map
        this.renderer = map != null ? new MapEntitiesRenderer(map) : null
        this.moment = initialMoment
    }

    async reRender() {
        if (this.map == null || this.renderer == null) return
        await this.retriever.fetchFrame(this.map.getBounds(), this.map.getZoom(), this.moment)
        const routes = this.store.routes
        this.store.journeys.forEach((j) => {
            const route = routes.get(j.routeId)
            if (route == null) return
            recalculateVehiclePosition(this.moment, j, route)
            if (j.position == null) return
            const firstRender = j.vehicleMarker == null
            this.renderer!.renderVehicle(j)
            if (firstRender) {
                j.vehicleMarker!.addEventListener('click', () => this.onJourneyClick(j, route))
            }
        })
    }
    debouncedReRender = debounce(() => this.reRender(), 400)

    private async fetchFocusedJourney(journeyId: number) {
        const journey = await this.retriever.fetchJourney(journeyId)
        if (journey == null || this.focusedJourney == null) return
        this.focusedJourney.journey = journey
        this.focusedJourneyListeners.forEach((listener) => listener(journey))
    }

    onJourneyClick(journey: RenderedMapJourney, route: RenderedMapRoute) {
        let renderRoute = true
        if (this.focusedJourney != null) {
            if (this.focusedJourney.journeyId === journey.relationalId) return
            if (this.focusedJourney.mapRoute.relationalId !== route.relationalId) {
                this.renderer!.clearRenderedRoute(this.focusedJourney.mapRoute)
            } else {
                renderRoute = false
            }
        }
        this.focusedJourney = {
            journeyId: journey.relationalId,
            journey: null,
            mapRoute: route,
        }
        if (this.focusedJourney.journey == null) {
            this.fetchFocusedJourney(journey.relationalId)
        }
        if (renderRoute) {
            this.renderer!.renderRoute(route)
        }
        this.map!.flyToBounds(route.featureGroup!.getBounds(), {
            duration: 0.5,
            easeLinearity: 0.8,
            animate: true,
            padding: [50, 50],
        })
    }

    clearFocusedJourney() {
        if (this.focusedJourney == null) return
        if (this.focusedJourney.journey != null) {
            this.focusedJourneyListeners.forEach((listener) => listener(null))
        }
        this.renderer!.clearRenderedRoute(this.focusedJourney.mapRoute)
        this.focusedJourney = null
    }

    /*
     *   Getters and Setters
     */

    setMap(map: L.Map) {
        this.map = map
        this.renderer = new MapEntitiesRenderer(map)
        this.reRender()
        map.on('move', this.debouncedReRender)
        map.on('zoom', this.debouncedReRender)
    }

    getMoment() {
        return new Date(this.moment)
    }
    setMoment(moment: Date) {
        if (moment.getTime() === this.moment.getTime()) return
        this.moment = moment
        this.momentListeners.forEach((listener) => listener(moment))
    }

    /*
     *   Listener registration
     */

    addMomentListener(listener: (moment: Date) => void) {
        this.momentListeners.push(listener)
    }
    removeMomentListener(listener: (moment: Date) => void) {
        this.momentListeners = this.momentListeners.filter((l) => l !== listener)
    }

    addFocusedJourneyListener(listener: (focused: JourneyWithDatesAndTimes | null) => void) {
        this.focusedJourneyListeners.push(listener)
    }
    removeFocusedJourneyListener(listener: (focused: JourneyWithDatesAndTimes | null) => void) {
        this.focusedJourneyListeners = this.focusedJourneyListeners.filter((l) => l !== listener)
    }
}
