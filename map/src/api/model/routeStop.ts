import type { PhysicalStop } from "@/api/model/physicalStop"

type RouteStop = {
    physicalStop: PhysicalStop,
    pointSequenceIndex: number,
}

export type { RouteStop }
