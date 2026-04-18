import type { LatLngTuple } from 'leaflet'
import type { RenderedMapJourney, RenderedMapRoute } from '@/services/mapEntitiesStore'
import { getInterpolationDataFromRouteFractions } from '@/services/interpolatePositions'
import L from 'leaflet'

const outlineColor = '#222'
const dropSvgStrokeWidth = '2'
const dropSvg = `
<svg viewBox="0 -0.5 17 17" style="display: block; width: 100%; height: 100%;">
    <g stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
        <path d="M3,10.333 C3,13.463 5.427,16 8.418,16 C11.41,16 14,13.463 14,10.333 C14,7.204 8.418,0 8.418,0 C8.418,0 3,7.204 3,10.333 Z" stroke="${outlineColor}" stroke-width="${dropSvgStrokeWidth}" fill="currentColor"/>
    </g>
</svg>
`
const dropSvgIconSize = 16

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

export class MapEntitiesRenderer {
    map: L.Map

    constructor(map: L.Map) {
        this.map = map
    }

    private getColor(key: number) {
        const index = (key * 5) % colorPalette.length
        return colorPalette[index]!
    }

    highlightStop(route: RenderedMapRoute, stopOrder: number) {
        if (route.stops == null) return
        route.stops[stopOrder]![1]!.openTooltip()
    }

    deHighlightStop(route: RenderedMapRoute, stopOrder: number) {
        if (route.stops == null) return
        route.stops[stopOrder]![1]!.closeTooltip()
    }

    clearRenderedRoute(route: RenderedMapRoute) {
        if (route.featureGroup == null) return
        route.featureGroup.remove()
        route.featureGroup = null
        route.stops = null
    }

    renderRoute(route: RenderedMapRoute, lineVersionId: number, stopNames: string[]) {
        if (route.featureGroup != null) return
        route.color = this.getColor(lineVersionId)
        route.featureGroup = L.featureGroup()
        route.featureGroup.addLayer(
            L.polyline(route.pointSequence, {
                color: outlineColor,
                weight: 4,
            }),
        )
        route.featureGroup.addLayer(
            L.polyline(route.pointSequence, {
                color: route.color,
                weight: 3,
            }),
        )

        route.stops = []
        getInterpolationDataFromRouteFractions(
            route.routeStopFractions,
            route.pointSequence,
            route.totalDistance,
        ).forEach((pointData) => {
            const position = pointData.position as LatLngTuple
            const stopMarkers = []
            stopMarkers.push(
                L.circleMarker(position, {
                    radius: 5,
                    color: outlineColor,
                    fillColor: outlineColor,
                    fillOpacity: 1,
                    weight: 4,
                }),
            )
            stopMarkers.push(
                L.circleMarker(position, {
                    radius: 5,
                    color: route.color!,
                    fillColor: 'white',
                    fillOpacity: 1,
                    weight: 3,
                }),
            )
            stopMarkers.forEach((marker) => route.featureGroup!.addLayer(marker))
            route.stops!.push(stopMarkers)
        })

        for (let i = 0; i < stopNames.length; i++) {
            const marker = route.stops[i]![1]!
            marker.bindTooltip(stopNames[i]!, {
                direction: 'top',
                offset: [0, -5],
            })
        }

        route.featureGroup.addTo(this.map)
    }

    private createVehicleIcon(journey: RenderedMapJourney) {
        return L.divIcon({
            className: 'vehicle-icon',
            html: `<div style="color: ${journey.color}; transform: rotate(${journey.azimuth}deg); width: 100%; height: 100%;">${dropSvg}</div>`,
            iconSize: [dropSvgIconSize, dropSvgIconSize],
            iconAnchor: [dropSvgIconSize / 2, dropSvgIconSize / 2],
        })
    }

    renderVehicle(journey: RenderedMapJourney) {
        if (journey.position == null || journey.azimuth == null) {
            if (journey.vehicleMarker != null) {
                journey.vehicleMarker.remove()
                journey.vehicleMarker = null
            }
            return
        }
        journey.color = this.getColor(journey.lineVersionId)
        if (journey.vehicleMarker) {
            journey.vehicleMarker.setLatLng(journey.position as LatLngTuple)
            journey.vehicleMarker.setIcon(this.createVehicleIcon(journey))
            return
        }
        journey.vehicleMarker = L.marker(journey.position as LatLngTuple, {
            icon: this.createVehicleIcon(journey),
        }).addTo(this.map)
    }

    clearRenderedVehicle(journey: RenderedMapJourney) {
        if (journey.vehicleMarker == null) return
        journey.vehicleMarker.remove()
        journey.vehicleMarker = null
    }
}
