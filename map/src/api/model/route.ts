import type { RouteStop } from "@/api/model/routeStop"

type Route = {
    routeId: string,
    pointSequence: number[][],
    routeStops: RouteStop[],
}

export type { Route }
