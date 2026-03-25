import type { JourneysOperatingInFrame, MapRawRoute, MapRoute } from '@/api/model/journeysOperatingInFrame'
import type { PositionedMapJourneyWithDates } from '@/services/interpolatePositionsForDaySpecificJourneys'
import type { LatLngTuple } from 'leaflet'
import { calculateVehiclePositions, getPositionFromRouteFractions } from '@/services/interpolatePositionsForDaySpecificJourneys'
import { toMapJourneyWithDates, toMapRoute } from '@/services/toDeserializedTypes'
import L from 'leaflet'
import JourneyApi from '@/api/journeyApi'

type RenderedMapJourney = PositionedMapJourneyWithDates & {
    vehicleMarker: L.CircleMarker | null
}

type CacheEntry = {
    startingJourneys: Map<number, RenderedMapJourney>
    continuingJourneys: Map<number, RenderedMapJourney>
    routes: Map<number, MapRoute>
}

const positionedJourneysForCurrentDay: CacheEntry = {
    startingJourneys: new Map(),
    continuingJourneys: new Map(),
    routes: new Map(),
}

function renderRoute(map: L.Map, route: MapRoute, journey: RenderedMapJourney) {
    L.geoJSON(
        route.pointSequence,
        {
            style: { color: 'red' }
        }
    ).addTo(map)
    getPositionFromRouteFractions(route.routeStops, route.pointSequence.coordinates, route.totalDistance)
        .forEach((pointCoordinates, idx) => {
            L.circleMarker(
                [pointCoordinates[1], pointCoordinates[0]] as LatLngTuple,
                {
                    radius: 4,
                    color: 'red',
                    fillColor: 'white',
                    fillOpacity: 1,
                    weight: 3,
                }
            ).addTo(map)
            .addEventListener('click', () => {
                console.log(journey.schedule[idx])
            })
        })
}

function renderVehicle(map: L.Map, journey: RenderedMapJourney) {
    if (journey.position == null) return
    renderRoute(map, positionedJourneysForCurrentDay.routes.get(journey.routeId!)!, journey)
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
        }
    ).addTo(map)
}

function updateRoutes(newRoutes: MapRawRoute[]) {
    for (const route of newRoutes) {
        if (positionedJourneysForCurrentDay.routes.has(route.relationalId)) continue
        positionedJourneysForCurrentDay.routes.set(route.relationalId, toMapRoute(route))
    }
}

function toRenderedJourney(journey: PositionedMapJourneyWithDates): RenderedMapJourney {
    return {
        ...journey,
        vehicleMarker: null,
    }
}

function updateJourneys(moment: Date, journeys: JourneysOperatingInFrame) {
    updateRoutes(journeys.routes)
    const positionedNewJourneys = calculateVehiclePositions(
        moment,
        journeys.startingThisDay.filter(r => !positionedJourneysForCurrentDay.startingJourneys.has(r.relationalId)).map(toMapJourneyWithDates),
        journeys.continuingThisDay.filter(r => !positionedJourneysForCurrentDay.continuingJourneys.has(r.relationalId)).map(toMapJourneyWithDates),
        positionedJourneysForCurrentDay.routes,
    )
    for (const journey of positionedNewJourneys.startingJourneys) {
        positionedJourneysForCurrentDay.startingJourneys.set(journey.relationalId, toRenderedJourney(journey))
    }
    for (const journey of positionedNewJourneys.continuingJourneys) {
        positionedJourneysForCurrentDay.continuingJourneys.set(journey.relationalId, toRenderedJourney(journey))
    }
}

function renderVehicles(map: L.Map, moment: Date, journeys: JourneysOperatingInFrame) {
    updateJourneys(moment, journeys)
    positionedJourneysForCurrentDay.startingJourneys.forEach(j => renderVehicle(map, j))
    positionedJourneysForCurrentDay.continuingJourneys.forEach(j => renderVehicle(map, j))
}


async function renderFrame(map: L.Map, moment: Date) {
    const bounds = map.getBounds()
    const journeys = await JourneyApi.getJourneysOperatingInFrame(
        bounds.getWest(),
        bounds.getSouth(),
        bounds.getEast(),
        bounds.getNorth(),
        map.getZoom(),
        moment
    )
    if (journeys) {
        renderVehicles(map, moment, journeys)
    }
}

export { renderFrame }
