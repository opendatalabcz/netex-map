import { MapEntitiesRenderer } from '@/services/mapEntitiesRenderer'
import { MapEntitiesRetriever } from '@/services/mapEntitiesRetriever'
import {
    MapEntitiesStore,
    type RenderedMapJourney,
    type RenderedMapRoute,
} from '@/services/mapEntitiesStore'
import { recalculateVehiclePosition } from '@/services/interpolatePositions'
import { debounce } from '@/util/debounce'
import type { JourneyDetailsWithTimes } from '@/api/model/journeyDetails'
import type { WallTimetableWithDates } from '@/api/model/wallTimetable'
import type { SearchLineVersionWithDates } from '@/api/model/searchLineVersions'

type FocusedJourney = {
    journeyId: number
    journeyDetails: JourneyDetailsWithTimes | null
    mapRoute: RenderedMapRoute
    highlightedStopOrder: number | null
}

type LineVersionSearchResult = {
    query: string
    lineVersions: SearchLineVersionWithDates[]
    lastLoadedPage: number
    pageSize: number
    totalPages: number
}

export class MapController {
    private store: MapEntitiesStore
    private retriever: MapEntitiesRetriever
    private map: L.Map | null
    private renderer: MapEntitiesRenderer | null
    private moment: Date
    private momentListeners: ((moment: Date) => void)[] = []
    private focusedJourney: FocusedJourney | null = null
    private focusedJourneyDetailsListeners: ((focused: JourneyDetailsWithTimes | null) => void)[] =
        []
    private selectedWallTimetable: WallTimetableWithDates | null = null
    private wallTimetableListeners: ((timetable: WallTimetableWithDates | null) => void)[] = []
    private lineVersionSearchResult: LineVersionSearchResult | null = null
    private lineVersionSearchResultListeners: ((
        lineVersions: SearchLineVersionWithDates[] | null,
    ) => void)[] = []
    private extendingLineSearch: boolean = false

    constructor(
        initialMoment: Date,
        store: MapEntitiesStore | null = null,
        map: L.Map | null = null,
    ) {
        this.store = store ?? new MapEntitiesStore()
        this.retriever = new MapEntitiesRetriever(this.store)
        this.map = map
        this.renderer = map != null ? new MapEntitiesRenderer(map) : null
        this.moment = initialMoment
    }

    /*
     *   Map contents
     */

    reRender() {
        if (this.renderer == null) return
        const routes = this.store.routes
        this.store.journeys.forEach((j) => {
            const route = routes.get(j.routeId)
            if (route == null) return
            recalculateVehiclePosition(this.moment, j, route)
            if (j.position == null) return
            const firstRender = j.vehicleMarker == null
            this.renderer!.renderVehicle(j)
            if (firstRender) {
                j.vehicleMarker!.addEventListener('click', () => this.onJourneyClick(j, route))
            }
        })
    }
    private async fetchFrame() {
        if (this.map == null) return
        await this.retriever.fetchFrame(
            this.map.getBounds().pad(0.25),
            this.map.getZoom(),
            this.moment,
        )
        this.reRender()
    }
    debouncedFrameFetch = debounce(() => this.fetchFrame(), 400)

    /*
     *   WallTimetable
     */

    async onWallTimetableSelected(lineVersionId: number | null) {
        if (lineVersionId == null) {
            this.clearSelectedWallTimetable()
            return
        }
        const timetable = await this.retriever.fetchWallTimetable(lineVersionId)
        if (timetable == null) return
        this.selectedWallTimetable = timetable
        this.wallTimetableListeners.forEach((listener) => listener(timetable))
    }

    clearSelectedWallTimetable() {
        if (this.selectedWallTimetable == null) return
        this.selectedWallTimetable = null
        this.wallTimetableListeners.forEach((listener) => listener(null))
    }

    private async getLineVersionSearch(query: string, pageNumber: number) {
        const pageSize = 10
        const searchPage = await this.retriever.searchLineVersions(query, pageSize, pageNumber)
        console.log(searchPage)
        if (searchPage == null) return
        if (pageNumber === 0) {
            this.lineVersionSearchResult = {
                query: query,
                lineVersions: searchPage.content,
                lastLoadedPage: pageNumber,
                pageSize: pageSize,
                totalPages: searchPage.page.totalPages,
            }
        } else if (this.lineVersionSearchResult?.query === query) {
            this.lineVersionSearchResult.lineVersions.push(...searchPage.content)
            this.lineVersionSearchResult.lastLoadedPage = pageNumber
        } else {
            return
        }
        this.lineVersionSearchResultListeners.forEach((listener) =>
            listener(this.lineVersionSearchResult!.lineVersions),
        )
    }
    debouncedLineVersionSearch = debounce(
        (query) => this.getLineVersionSearch(query + '', 0),
        250,
    ) as (query: string) => void

    clearLineVersionSearch() {
        if (this.lineVersionSearchResult == null) return
        this.lineVersionSearchResult = null
        this.lineVersionSearchResultListeners.forEach((listener) => listener(null))
    }

    async extendLineVersionSearch() {
        if (
            this.extendingLineSearch ||
            this.lineVersionSearchResult == null ||
            this.lineVersionSearchResult.lastLoadedPage ===
                this.lineVersionSearchResult.totalPages - 1
        )
            return
        this.extendingLineSearch = true
        await this.getLineVersionSearch(
            this.lineVersionSearchResult.query,
            this.lineVersionSearchResult.lastLoadedPage + 1,
        )
        this.extendingLineSearch = false
    }

    /*
     *   JourneyDetails
     */

    private async fetchJourneyDetails(journeyId: number) {
        const details = await this.retriever.fetchJourneyDetails(journeyId)
        if (details == null || this.focusedJourney == null) return
        this.focusedJourney.journeyDetails = details
        this.focusedJourneyDetailsListeners.forEach((listener) => listener(details))
        if (this.renderer == null) return
        this.renderer.bindStopNames(
            this.focusedJourney.mapRoute,
            details.stops.map((s) => s.name),
        )
    }

    onJourneyClick(journey: RenderedMapJourney, route: RenderedMapRoute) {
        let renderRoute = true
        if (this.focusedJourney != null) {
            if (this.focusedJourney.journeyId === journey.relationalId) return
            if (this.focusedJourney.mapRoute.relationalId !== route.relationalId) {
                this.renderer!.clearRenderedRoute(this.focusedJourney.mapRoute)
            } else {
                renderRoute = false
            }
        }
        this.focusedJourney = {
            journeyId: journey.relationalId,
            journeyDetails: null,
            mapRoute: route,
            highlightedStopOrder: null,
        }
        this.fetchJourneyDetails(journey.relationalId)
        if (renderRoute) {
            this.renderer!.renderRoute(route)
        }
        this.map!.flyToBounds(route.featureGroup!.getBounds(), {
            duration: 0.5,
            easeLinearity: 0.8,
            animate: true,
            paddingTopLeft: [20, 20],
            paddingBottomRight: [420, 20],
        })
    }

    clearJourneyDetails() {
        if (this.focusedJourney == null) return
        if (this.focusedJourney.journeyDetails != null) {
            this.focusedJourneyDetailsListeners.forEach((listener) => listener(null))
        }
        this.renderer!.clearRenderedRoute(this.focusedJourney.mapRoute)
        this.focusedJourney = null
    }

    highlightJourneyDetailsStop(stopOrder: number) {
        if (this.focusedJourney == null) return
        if (this.focusedJourney.highlightedStopOrder != null) {
            this.renderer!.deHighlightStop(
                this.focusedJourney.mapRoute,
                this.focusedJourney.highlightedStopOrder,
            )
            if (this.focusedJourney.highlightedStopOrder === stopOrder) {
                this.focusedJourney.highlightedStopOrder = null
                return
            }
        }
        this.focusedJourney.highlightedStopOrder = stopOrder
        this.renderer!.highlightStop(this.focusedJourney.mapRoute, stopOrder)
        const offsetStopPosition = this.focusedJourney.mapRoute
            .stops![stopOrder]![0]!.getLatLng()
            .clone()
        const mapBounds = this.map!.getBounds()
        offsetStopPosition.lng += (mapBounds.getEast() - mapBounds.getWest()) * 0.1
        this.map!.flyTo(offsetStopPosition, undefined, {
            duration: 0.5,
            easeLinearity: 0.8,
            animate: true,
        })
    }

    /*
     *   Getters and Setters
     */

    setMap(map: L.Map) {
        this.map = map
        this.renderer = new MapEntitiesRenderer(map)
        this.fetchFrame()
        map.on('move', () => this.debouncedFrameFetch())
        map.on('zoom', () => this.debouncedFrameFetch())
    }

    getMoment() {
        return new Date(this.moment)
    }
    setMoment(moment: Date) {
        if (moment.getTime() === this.moment.getTime()) return
        this.moment = moment
        this.momentListeners.forEach((listener) => listener(moment))
    }

    /*
     *   Listener registration
     */

    addMomentListener(listener: (moment: Date) => void) {
        this.momentListeners.push(listener)
    }
    removeMomentListener(listener: (moment: Date) => void) {
        this.momentListeners = this.momentListeners.filter((l) => l !== listener)
    }

    addJourneyDetailsListener(listener: (focused: JourneyDetailsWithTimes | null) => void) {
        this.focusedJourneyDetailsListeners.push(listener)
    }
    removeJourneyDetailsListener(listener: (focused: JourneyDetailsWithTimes | null) => void) {
        this.focusedJourneyDetailsListeners = this.focusedJourneyDetailsListeners.filter(
            (l) => l !== listener,
        )
    }

    addWallTimetableListener(listener: (timetable: WallTimetableWithDates | null) => void) {
        this.wallTimetableListeners.push(listener)
    }
    removeWallTimetableListener(listener: (timetable: WallTimetableWithDates | null) => void) {
        this.wallTimetableListeners = this.wallTimetableListeners.filter((l) => l !== listener)
    }

    addLineVersionSearchListener(
        listener: (lineVersions: SearchLineVersionWithDates[] | null) => void,
    ) {
        this.lineVersionSearchResultListeners.push(listener)
    }
    removeLineVersionSearchListener(
        listener: (lineVersions: SearchLineVersionWithDates[] | null) => void,
    ) {
        this.lineVersionSearchResultListeners = this.lineVersionSearchResultListeners.filter(
            (l) => l !== listener,
        )
    }
}
