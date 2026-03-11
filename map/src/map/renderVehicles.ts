import type { JourneysOperatingInDay } from '@/api/model/journeysOperatingInDay'
import type { Route } from '@/api/model/route'
import type { PositionedJourneys } from '@/services/interpolatePositionsForDaySpecificJourneys'
import type { LatLngTuple } from 'leaflet'
import { calculateVehiclePositions } from '@/services/interpolatePositionsForDaySpecificJourneys'
import { toDaySpecificJourneyWithDates } from '@/services/toDateTypes'
import L from 'leaflet'

let positionedJourneysForCurrentDay: PositionedJourneys = {
    startingJourneys: [],
    continuingJourneys: [],
}
let routeMapForCurrentDay: Map<number, Route> = new Map()

function renderVehicles(map: L.Map, moment: Date, journeys: JourneysOperatingInDay) {
    routeMapForCurrentDay = new Map(journeys.routes.map((r) => [r.relationalId, r]))
    positionedJourneysForCurrentDay = calculateVehiclePositions(
        moment,
        journeys.startingThisDay.map(toDaySpecificJourneyWithDates),
        journeys.continuingThisDay.map(toDaySpecificJourneyWithDates),
        routeMapForCurrentDay,
    )
    positionedJourneysForCurrentDay.startingJourneys.forEach((journey) => {
        if (journey.position == null) return
        L.circle(journey.position as LatLngTuple, { radius: 10, color: 'blue' }).addTo(map)
    })
    console.log(positionedJourneysForCurrentDay)
}

export { renderVehicles }
