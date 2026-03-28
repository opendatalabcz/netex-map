import type { MapRoute } from '@/api/model/journeysOperatingInFrame'
import type { LatLngTuple } from 'leaflet'
import {
    getInterpolationDataFromRouteFractions,
    recalculateVehiclePosition,
} from '@/services/interpolatePositions'
import L from 'leaflet'
import { MapEntitiesStore, type RenderedMapJourney } from '@/services/mapEntitiesStore'

const dropSvg = `
<svg viewBox="0 -0.5 17 17" style="display: block; width: 100%; height: 100%;">
    <g stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
        <path d="M3,10.333 C3,13.463 5.427,16 8.418,16 C11.41,16 14,13.463 14,10.333 C14,7.204 8.418,0 8.418,0 C8.418,0 3,7.204 3,10.333 Z" stroke="#222" stroke-width="2" fill="currentColor"/>
    </g>
</svg>
`

const colorPalette = [
    'hsl(0, 76%, 52%)',
    'hsl(32, 92%, 54%)',
    'hsl(50, 100%, 62%)',
    'hsl(96, 84%, 41%)',
    'hsl(184, 80%, 61%)',
    'hsl(230, 100%, 59%)',
    'hsl(272, 100%, 60%)',
    'hsl(290, 100%, 73%)',
]

const carSvgIconSize = 16

export class MapEntitiesRenderer {
    map: L.Map
    mapEntriesStore: MapEntitiesStore
    renderedRouteElements: L.Layer[] = []

    constructor(map: L.Map, mapEntriesStore: MapEntitiesStore) {
        this.map = map
        this.mapEntriesStore = mapEntriesStore
    }

    clearRenderedRoute() {
        for (const el of this.renderedRouteElements) {
            el.remove()
        }
        this.renderedRouteElements = []
    }

    renderRoute(route: MapRoute, journey: RenderedMapJourney) {
        this.clearRenderedRoute()
        const masterLine = L.geoJSON(route.pointSequence, {
            style: {
                color: '#222',
                weight: 4,
            },
        }).addTo(this.map)
        this.renderedRouteElements.push(masterLine)
        this.renderedRouteElements.push(
            L.geoJSON(route.pointSequence, {
                style: {
                    color: journey.color!,
                    weight: 3,
                },
            }).addTo(this.map),
        )

        getInterpolationDataFromRouteFractions(
            route.routeStops,
            route.pointSequence.coordinates,
            route.totalDistance,
        ).forEach((pointData) => {
            const position = [pointData.position[1], pointData.position[0]] as LatLngTuple
            this.renderedRouteElements.push(
                L.circleMarker(position, {
                    radius: 5,
                    color: '#222',
                    fillColor: '#222',
                    fillOpacity: 1,
                    weight: 4,
                }).addTo(this.map),
            )
            this.renderedRouteElements.push(
                L.circleMarker(position, {
                    radius: 5,
                    color: journey.color!,
                    fillColor: 'white',
                    fillOpacity: 1,
                    weight: 3,
                }).addTo(this.map),
            )
        })

        this.map.flyToBounds(masterLine.getBounds(), {
            duration: 1,
            easeLinearity: 0.8,
            animate: true,
            padding: [50, 50],
        })
    }

    createVehicleIcon(journey: RenderedMapJourney) {
        return L.divIcon({
            className: 'vehicle-icon',
            html: `<div style="color: ${journey.color}; transform: rotate(${journey.azimuth}deg); width: 100%; height: 100%;">${dropSvg}</div>`,
            iconSize: [carSvgIconSize, carSvgIconSize],
            iconAnchor: [carSvgIconSize / 2, carSvgIconSize / 2],
        })
    }

    renderVehicle(journey: RenderedMapJourney) {
        if (journey.position == null || journey.azimuth == null) return
        if (journey.color == null)
            journey.color = colorPalette[journey.routeId! % colorPalette.length]!
        if (journey.vehicleMarker) {
            journey.vehicleMarker.setLatLng([
                journey.position[1],
                journey.position[0],
            ] as LatLngTuple)
            journey.vehicleMarker.setIcon(this.createVehicleIcon(journey))
            return
        }
        journey.vehicleMarker = L.marker(
            [journey.position[1], journey.position[0]] as LatLngTuple,
            { icon: this.createVehicleIcon(journey) },
        )
            .addTo(this.map)
            .addEventListener('click', () => {
                this.renderRoute(this.mapEntriesStore.routes.get(journey.routeId!)!, journey)
            })
    }

    async renderFrame(moment: Date) {
        const routes = this.mapEntriesStore.routes
        this.mapEntriesStore.journeys.forEach((j) => {
            if (j.routeId == null) return
            const route = routes.get(j.routeId)
            if (route == null) return
            recalculateVehiclePosition(moment, j, route)
            if (j.position == null) return
            this.renderVehicle(j)
        })
    }
}
