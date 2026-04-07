import { MapEntitiesRenderer } from '@/services/mapEntitiesRenderer'
import { MapEntitiesRetriever } from '@/services/mapEntitiesRetriever'
import {
    MapEntitiesStore,
    type RenderedMapJourney,
    type RenderedMapRoute,
} from '@/services/mapEntitiesStore'
import { recalculateVehiclePosition } from '@/services/interpolatePositions'
import { debounce } from '@/util/debounce'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'

type FocusedJourney = {
    journeyId: number
    journeyDetails: JourneyDetailsWithTimes | null
    mapRoute: RenderedMapRoute
    highlightedStopOrder: number | null
}

export class MapController {
    private store: MapEntitiesStore
    private retriever: MapEntitiesRetriever
    private map: L.Map | null
    private renderer: MapEntitiesRenderer | null
    private moment: Date
    private momentListeners: ((moment: Date) => void)[] = []
    private focusedJourney: FocusedJourney | null = null
    private focusedJourneyDetailsListeners: ((focused: JourneyDetailsWithTimes | null) => void)[] =
        []

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

    private async fetchJourneyDetails(journeyId: number) {
        const details = await this.retriever.fetchJourneyDetails(journeyId)
        if (details == null || this.focusedJourney == null) return
        this.focusedJourney.journeyDetails = details
        this.focusedJourneyDetailsListeners.forEach((listener) => listener(details))
        if (this.renderer == null) return
        this.renderer.bindStopNames(this.focusedJourney.mapRoute, details.stops.map((s) => s.name))
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
            journeyDetails: null,
            mapRoute: route,
            highlightedStopOrder: null,
        }
        this.fetchJourneyDetails(journey.relationalId)
        if (renderRoute) {
            this.renderer!.renderRoute(route)
        }
        this.map!.flyToBounds(route.featureGroup!.getBounds(), {
            duration: 0.5,
            easeLinearity: 0.8,
            animate: true,
            paddingTopLeft: [20, 20],
            paddingBottomRight: [420, 20],
        })
    }

    clearJourneyDetails() {
        if (this.focusedJourney == null) return
        if (this.focusedJourney.journeyDetails != null) {
            this.focusedJourneyDetailsListeners.forEach((listener) => listener(null))
        }
        this.renderer!.clearRenderedRoute(this.focusedJourney.mapRoute)
        this.focusedJourney = null
    }

    highlightJourneyDetailsStop(stopOrder: number) {
        if (this.focusedJourney == null) return
        if (this.focusedJourney.highlightedStopOrder != null) {
            this.renderer!.deHighlightStop(this.focusedJourney.mapRoute, this.focusedJourney.highlightedStopOrder)
            if (this.focusedJourney.highlightedStopOrder === stopOrder) {
                this.focusedJourney.highlightedStopOrder = null
                return
            }
        }
        this.focusedJourney.highlightedStopOrder = stopOrder
        this.renderer!.highlightStop(this.focusedJourney.mapRoute, stopOrder)
        const offsetStopPosition = this.focusedJourney.mapRoute.stops![stopOrder]![0]!.getLatLng().clone()
        const mapBounds = this.map!.getBounds()
        offsetStopPosition.lng += (mapBounds.getEast() - mapBounds.getWest()) * 0.1
        this.map!.flyTo(offsetStopPosition, undefined,  {
            duration: 0.5,
            easeLinearity: 0.8,
            animate: true,
        })
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

    addJourneyDetailsListener(listener: (focused: JourneyDetailsWithTimes | null) => void) {
        this.focusedJourneyDetailsListeners.push(listener)
    }
    removeJourneyDetailsListener(listener: (focused: JourneyDetailsWithTimes | null) => void) {
        this.focusedJourneyDetailsListeners = this.focusedJourneyDetailsListeners.filter(
            (l) => l !== listener,
        )
    }
}
