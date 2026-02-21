import type { LineVersion } from "@/api/model/lineVersion"
import type { OperatingPeriod } from "@/api/model/operatingPeriod"
import type { Route } from "@/api/model/route"
import type { ScheduledStop } from "@/api/model/scheduledStop"


type Journey = {
    journeyId: string,
    lineVersion: LineVersion,
    journeyPatternId: string,
    schedule: ScheduledStop[],
    operatingPeriods: OperatingPeriod[],
    route: Route | null,
}


export type { Journey }
