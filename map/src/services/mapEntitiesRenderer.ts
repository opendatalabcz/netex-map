import type { MapRoute } from '@/api/model/journeysOperatingInFrame'
import type { LatLngTuple } from 'leaflet'
import {
    getPositionFromRouteFractions,
    recalculateVehiclePosition,
} from '@/services/interpolatePositions'
import L from 'leaflet'
import JourneyApi from '@/api/journeyApi'
import { MapEntitiesStore, type RenderedMapJourney } from '@/services/mapEntitiesStore'

export class MapEntitiesRenderer {
    map: L.Map
    mapEntriesStore: MapEntitiesStore

    constructor(map: L.Map, mapEntriesStore?: MapEntitiesStore | undefined) {
        this.map = map
        this.mapEntriesStore = mapEntriesStore ?? new MapEntitiesStore()
    }

    renderRoute(route: MapRoute, journey: RenderedMapJourney) {
        L.geoJSON(route.pointSequence, {
            style: { color: 'red' },
        }).addTo(this.map)
        getPositionFromRouteFractions(
            route.routeStops,
            route.pointSequence.coordinates,
            route.totalDistance,
        ).forEach((pointCoordinates, idx) => {
            L.circleMarker([pointCoordinates[1], pointCoordinates[0]] as LatLngTuple, {
                radius: 4,
                color: 'red',
                fillColor: 'white',
                fillOpacity: 1,
                weight: 3,
            })
                .addTo(this.map)
                .addEventListener('click', () => {
                    console.log(journey.schedule[idx])
                })
        })
    }

    renderVehicle(journey: RenderedMapJourney) {
        if (journey.position == null) return
        // this.renderRoute(this.mapEntriesStore.routes.get(journey.routeId!)!, journey)
        if (journey.vehicleMarker) {
            journey.vehicleMarker.setLatLng([journey.position[1], journey.position[0]] as LatLngTuple)
            return
        }
        journey.vehicleMarker = L.circleMarker(
            [journey.position[1], journey.position[0]] as LatLngTuple,
            {
                radius: 5,
                color: 'blue',
                fillOpacity: 1,
            },
        ).addTo(this.map)
    }

    async renderFrame(moment: Date) {
        const bounds = this.map.getBounds()
        const frame = await JourneyApi.getJourneysOperatingInFrame(
            bounds.getWest(),
            bounds.getSouth(),
            bounds.getEast(),
            bounds.getNorth(),
            this.map.getZoom(),
            moment,
        )
        if (!frame) return

        this.mapEntriesStore.addRoutes(frame.routes)
        this.mapEntriesStore.addLineVersions(frame.lineVersions)
        this.mapEntriesStore.addJourneys(frame.journeys)
        const routes = this.mapEntriesStore.routes
        this.mapEntriesStore.journeys.forEach((j) => {
            recalculateVehiclePosition(moment, j, routes)
            this.renderVehicle(j)
        })
    }
}
