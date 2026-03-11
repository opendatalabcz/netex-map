import type { RouteStop } from '@/api/model/routeStop'

type Route = {
    relationalId: number
    externalId: string
    pointSequence: number[][]
    routeStops: RouteStop[]
}

export type { Route }
