import HttpRequestSender from '@/api/httpRequestSender'
import type { WallTimetable } from '@/api/model/wallTimetable'
import type { Page } from '@/api/model/page'
import type { SearchLineVersion } from '@/api/model/searchLineVersions'

const LINE_VERSION_URI = 'line-version'
const WALL_TIMETABLE_LOCATION = 'wall'

const LineVersionApi = {
    getLineVersionWallTimetable(lineVersionId: number): Promise<WallTimetable | null | undefined> {
        return HttpRequestSender.get(
            [LINE_VERSION_URI, lineVersionId.toString(), WALL_TIMETABLE_LOCATION],
            null,
        )
    },

    searchLineVersions(
        query: string,
        pageSize?: number | undefined,
        pageNumber?: number | undefined,
    ): Promise<Page<SearchLineVersion> | null | undefined> {
        const options: Record<string, unknown> = { query: query }
        if (pageSize != undefined) options.size = pageSize
        if (pageNumber != undefined) options.page = pageNumber
        return HttpRequestSender.get([LINE_VERSION_URI], options)
    },
}

export default LineVersionApi
