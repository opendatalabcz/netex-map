import type { PhysicalStop } from '@/api/model/physicalStop'

type RouteStop = {
    physicalStop: PhysicalStop
    routeFraction: number
}

export type { RouteStop }
