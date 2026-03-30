import { MapEntitiesRenderer } from '@/services/mapEntitiesRenderer'
import { MapEntitiesRetriever } from '@/services/mapEntitiesRetriever'
import { MapEntitiesStore, type RenderedMapRoute } from '@/services/mapEntitiesStore'
import { recalculateVehiclePosition } from '@/services/interpolatePositions'
import { debounce } from '@/util/debounce'

export class MapController {
    private store: MapEntitiesStore
    private retriever: MapEntitiesRetriever
    private map: L.Map | null
    private renderer: MapEntitiesRenderer | null
    private moment: Date
    private momentListeners: ((moment: Date) => void)[] = []
    private focusedRoute: RenderedMapRoute | null = null

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

    addMomentListener(listener: (moment: Date) => void) {
        this.momentListeners.push(listener)
    }
    removeMomentListener(listener: (moment: Date) => void) {
        this.momentListeners = this.momentListeners.filter((l) => l !== listener)
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
                j.vehicleMarker!.addEventListener('click', () => this.onRouteClick(route))
            }
        })
    }
    debouncedReRender = debounce(() => this.reRender(), 400)
    onRouteClick(route: RenderedMapRoute) {
        if (this.focusedRoute != null) {
            if (this.focusedRoute.relationalId === route.relationalId) return
            this.renderer!.clearRenderedRoute(this.focusedRoute)
        }
        this.renderer!.renderRoute(route)
        this.focusedRoute = route
        this.map!.flyToBounds(route.featureGroup!.getBounds(), {
            duration: 0.5,
            easeLinearity: 0.8,
            animate: true,
            padding: [50, 50],
        })
    }
}
