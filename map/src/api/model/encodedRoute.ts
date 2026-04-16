export type EncodedRoute = {
    relationalId: number
    pointSequence: string
    totalDistance: number
    routeStopFractions: number[]
}

export type Route = {
    relationalId: number
    pointSequence: [number, number][]
    totalDistance: number
    routeStopFractions: number[]
}
