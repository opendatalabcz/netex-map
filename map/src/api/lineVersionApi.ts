import HttpRequestSender from '@/api/httpRequestSender'
import type { WallTimetable } from '@/api/model/wallTimetable'

const LINE_VERSION_URI = 'line-version'
const WALL_TIMETABLE_LOCATION = 'wall'

const LineVersionApi = {
    getLineVersionWallTimetable(lineVersionId: number): Promise<WallTimetable | null | undefined> {
        return HttpRequestSender.get(
            [LINE_VERSION_URI, lineVersionId.toString(), WALL_TIMETABLE_LOCATION],
            null,
        )
    },
}

export default LineVersionApi
