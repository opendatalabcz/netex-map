import type { MapRoute } from '@/api/model/journeysOperatingInFrame'
import type { LatLngTuple } from 'leaflet'
import {
    getInterpolationDataFromRouteFractions,
    recalculateVehiclePosition,
} from '@/services/interpolatePositions'
import L from 'leaflet'
import JourneyApi from '@/api/journeyApi'
import { MapEntitiesStore, type RenderedMapJourney } from '@/services/mapEntitiesStore'

const carSvg = `
<svg viewBox="0 -0.5 17 17" style="display: block; width: 100%; height: 100%;">
    <g stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
        <path d="M3,10.333 C3,13.463 5.427,16 8.418,16 C11.41,16 14,13.463 14,10.333 C14,7.204 8.418,0 8.418,0 C8.418,0 3,7.204 3,10.333 Z" fill="#0004ff"/>
    </g>
</svg>
`
const carSvgIconSize = 16

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
        getInterpolationDataFromRouteFractions(
            route.routeStops,
            route.pointSequence.coordinates,
            route.totalDistance,
        ).forEach((pointData, idx) => {
            L.circleMarker([pointData.position[1], pointData.position[0]] as LatLngTuple, {
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

    createVehicleIcon(azimuth: number) {
        return L.divIcon({
            className: 'vehicle-icon',
            html: `<div style="transform: rotate(${azimuth}deg); width: 100%; height: 100%;">${carSvg}</div>`,
            iconSize: [carSvgIconSize, carSvgIconSize],
            iconAnchor: [carSvgIconSize / 2, carSvgIconSize / 2],
        })
    }

    renderVehicle(journey: RenderedMapJourney) {
        if (journey.position == null || journey.azimuth == null) return
        // this.renderRoute(this.mapEntriesStore.routes.get(journey.routeId!)!, journey)
        if (journey.vehicleMarker) {
            journey.vehicleMarker.setLatLng([
                journey.position[1],
                journey.position[0],
            ] as LatLngTuple)
            journey.vehicleMarker.setIcon(this.createVehicleIcon(journey.azimuth))
            return
        }
        journey.vehicleMarker = L.marker(
            [journey.position[1], journey.position[0]] as LatLngTuple,
            {
                icon: this.createVehicleIcon(journey.azimuth),
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
