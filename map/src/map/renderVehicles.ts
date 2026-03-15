import type { JourneysOperatingInDay, MapRoute } from '@/api/model/journeysOperatingInDay'
import type { PositionedMapJourneyWithDates, PositionedJourneys } from '@/services/interpolatePositionsForDaySpecificJourneys'
import type { LatLngTuple } from 'leaflet'
import { calculateVehiclePositions } from '@/services/interpolatePositionsForDaySpecificJourneys'
import { toMapJourneyWithDates, toMapRoute } from '@/services/toDateTypes'
import L from 'leaflet'

let positionedJourneysForCurrentDay: PositionedJourneys = {
    startingJourneys: [],
    continuingJourneys: [],
}
let mapRoutesForCurrentDay: Map<number, MapRoute> = new Map()

function renderRoute(map: L.Map, route: MapRoute, journey: PositionedMapJourneyWithDates) {
    L.geoJSON(
        route.pointSequence,
        {
            style: { color: 'red' }
        }
    ).addTo(map)
    route.routeStops.forEach((stop, idx) => {
        const pointCoordinates = route.pointSequence.coordinates[stop.pointSequenceIndex]!
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

function renderVehicle(map: L.Map, journey: PositionedMapJourneyWithDates) {
    if (journey.position == null) return
    renderRoute(map, mapRoutesForCurrentDay.get(journey.routeId!)!, journey)
    L.circleMarker(
        [journey.position[1], journey.position[0]] as LatLngTuple,
        {
            radius: 5,
            color: 'blue',
            fillOpacity: 1,
        }
    ).addTo(map)
}

function renderVehicles(map: L.Map, moment: Date, journeys: JourneysOperatingInDay) {
    mapRoutesForCurrentDay = new Map(journeys.routes.map((r) => [r.relationalId, toMapRoute(r)]))
    positionedJourneysForCurrentDay = calculateVehiclePositions(
        moment,
        journeys.startingThisDay.map(toMapJourneyWithDates),
        journeys.continuingThisDay.map(toMapJourneyWithDates),
        mapRoutesForCurrentDay,
    )
    positionedJourneysForCurrentDay.startingJourneys.forEach(j => renderVehicle(map, j))
    positionedJourneysForCurrentDay.continuingJourneys.forEach(j => renderVehicle(map, j))
}

export { renderVehicles }
