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

type FetchQueueEntry = {
    bounds: L.LatLngBounds
    zoom: number
    moment: Date
    reRender: boolean
}

const LINE_VERSION_SEARCH_DEBOUNCE_DELAY = 250
const LINE_VERSION_SEARCH_PAGE_SIZE = 10
const MAP_FLY_DURATION = 0.5
const MAP_FLY_LINEARITY = 0.8
const FRAME_FETCH_FRAME_EXTRA_PAD_SCALE = 0.25
const FRAME_FETCH_DEBOUNCE_DELAY = 400
const FRAME_FETCH_MINUTES_IN_ADVANCE = 15
const STOP_FOCUS_HORIZONTAL_OFFSET_SCALE = 0.1
const ANIMATION_FRAME_REQUIRED_DELAY_IN_MILLIS = 500

export class MapController {
    private store: MapEntitiesStore
    private retriever: MapEntitiesRetriever
    private map: L.Map | null
    private renderer: MapEntitiesRenderer | null
    private moment: Date
    private momentListeners: ((moment: Date) => void)[] = []
    private momentKey: number
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
    private animationPlaying: boolean = false
    private animationPlayingListeners: ((playing: boolean) => void)[] = []
    private animationSpeed: number = 1
    private animationSpeedListeners: ((speed: number) => void)[] = []

    constructor(
        initialMoment: Date = new Date(import.meta.env.FE_INITIAL_MOMENT),
        store: MapEntitiesStore | null = null,
        map: L.Map | null = null,
    ) {
        this.store = store ?? new MapEntitiesStore()
        this.retriever = new MapEntitiesRetriever(this.store)
        this.map = map
        this.renderer = map != null ? new MapEntitiesRenderer(map) : null
        this.moment = initialMoment
        this.momentKey = this.store.getMomentKeyFor(initialMoment)
    }

    /*
     *   Map contents
     */

    reRender() {
        if (this.renderer == null) return
        const routes = this.store.routes()
        const journeysForMoment = this.store.journeysForMoment(this.moment)
        if (journeysForMoment == null) return
        for (const journey of journeysForMoment) {
            const route = routes.get(journey.routeId)
            if (route == null) continue
            recalculateVehiclePosition(this.moment, journey, route)
            const firstRender = journey.vehicleMarker == null
            this.renderer!.renderVehicle(journey)
            if (firstRender && journey.vehicleMarker != null) {
                journey.vehicleMarker.addEventListener('click', () =>
                    this.onJourneyClick(journey, route),
                )
            }
        }
    }

    private fetchQueue: FetchQueueEntry[] = []
    private fetching = false
    private async fetchFrame(moment: Date, reRender: boolean) {
        if (this.map == null) return
        const zoom = this.map.getZoom()
        this.fetchQueue.push({
            bounds: this.map.getBounds().pad(FRAME_FETCH_FRAME_EXTRA_PAD_SCALE),
            zoom: Number.isInteger(zoom) ? zoom : Number.parseInt(zoom + ''),
            moment: moment,
            reRender: reRender,
        })
        if (this.fetching) return
        this.fetching = true
        while (this.fetchQueue.length > 0) {
            const fetchRequest = this.fetchQueue.shift()!
            await this.retriever.fetchFrame(
                fetchRequest.bounds,
                fetchRequest.zoom,
                fetchRequest.moment,
            )
            if (fetchRequest.reRender) this.reRender()
        }
        this.fetching = false
    }
    debouncedFrameFetch = debounce(
        (moment, reRender) => this.fetchFrame(moment as Date, reRender as boolean),
        FRAME_FETCH_DEBOUNCE_DELAY,
    ) as (moment: Date, reRender: boolean) => void

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
        const searchPage = await this.retriever.searchLineVersions(
            query,
            LINE_VERSION_SEARCH_PAGE_SIZE,
            pageNumber,
        )
        if (searchPage == null) return
        if (pageNumber === 0) {
            this.lineVersionSearchResult = {
                query: query,
                lineVersions: searchPage.content,
                lastLoadedPage: pageNumber,
                pageSize: LINE_VERSION_SEARCH_PAGE_SIZE,
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
        LINE_VERSION_SEARCH_DEBOUNCE_DELAY,
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
            this.renderer!.renderRoute(route, journey.lineVersionId)
        }
        this.map!.flyToBounds(route.featureGroup!.getBounds(), {
            duration: MAP_FLY_DURATION,
            easeLinearity: MAP_FLY_LINEARITY,
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
        offsetStopPosition.lng +=
            (mapBounds.getEast() - mapBounds.getWest()) * STOP_FOCUS_HORIZONTAL_OFFSET_SCALE
        this.map!.flyTo(offsetStopPosition, undefined, {
            duration: MAP_FLY_DURATION,
            easeLinearity: MAP_FLY_LINEARITY,
            animate: true,
        })
    }

    /*
     *   Animation
     */

    private animationRequestingEnded = true
    private animationPreviousTimeStamp: DOMHighResTimeStamp | null = null
    animationStep(timestamp: DOMHighResTimeStamp) {
        if (!this.animationPlaying) {
            this.animationRequestingEnded = true
            this.animationPreviousTimeStamp = null
            return
        }
        if (this.animationPreviousTimeStamp == null) {
            this.animationPreviousTimeStamp = timestamp
            requestAnimationFrame((t) => this.animationStep(t))
            return
        }
        const elapsedMillis = timestamp - this.animationPreviousTimeStamp
        if (elapsedMillis < ANIMATION_FRAME_REQUIRED_DELAY_IN_MILLIS) {
            requestAnimationFrame((t) => this.animationStep(t))
            return
        }
        const newMoment = new Date(this.moment)
        newMoment.setMilliseconds(this.moment.getMilliseconds() + elapsedMillis * this.animationSpeed)
        this.setMoment(newMoment)
        this.animationPreviousTimeStamp = timestamp
        requestAnimationFrame((t) => this.animationStep(t))
    }

    /*
     *   Setters
     */

    async setMap(map: L.Map | null) {
        this.map = map
        if (map == null) return
        this.renderer = new MapEntitiesRenderer(map)
        map.on('move', () => this.debouncedFrameFetch(this.moment, true))
        map.on('zoom', () => this.debouncedFrameFetch(this.moment, true))
        await this.fetchFrame(this.moment, true)
    }

    private loadFrameInAdvance(moment: Date, momentKey?: number | undefined) {
        const momentBuffer = new Date(moment)
        momentBuffer.setMinutes(moment.getMinutes() + FRAME_FETCH_MINUTES_IN_ADVANCE)
        const bufferKey = this.store.getMomentKeyFor(momentBuffer)
        const usedMomentKey = momentKey ?? this.store.getMomentKeyFor(moment)
        if (usedMomentKey !== bufferKey) {
            this.fetchFrame(momentBuffer, false)
        }
    }
    async setMoment(moment: Date) {
        if (moment.getTime() === this.moment.getTime()) return
        this.moment = moment
        this.momentListeners.forEach((listener) => listener(moment))
        const momentKey = this.store.getMomentKeyFor(moment)
        if (this.momentKey !== momentKey) {
            /* TODO: Clear journeys from previous frame */
            this.fetchFrame(moment, true)
            this.loadFrameInAdvance(moment, momentKey)
            return
        }
        this.loadFrameInAdvance(moment, momentKey)
        this.reRender()
    }

    setAnimationPlaying(playing: boolean) {
        if (this.animationPlaying === playing) return
        this.animationPlaying = playing
        this.animationPlayingListeners.forEach((listener) => listener(playing))
        if (!playing || !this.animationRequestingEnded) return
        this.animationRequestingEnded = false
        requestAnimationFrame((t) => this.animationStep(t))
    }

    setAnimationSpeed(speed: number) {
        if (this.animationSpeed === speed) return
        this.animationSpeed = speed
        this.animationSpeedListeners.forEach((listener) => listener(speed))
    }

    /*
     *   Listener registration
     */

    addMomentListener(listener: (moment: Date) => void) {
        this.momentListeners.push(listener)
        listener(this.moment)
    }
    removeMomentListener(listener: (moment: Date) => void) {
        this.momentListeners = this.momentListeners.filter((l) => l !== listener)
    }

    addJourneyDetailsListener(listener: (focused: JourneyDetailsWithTimes | null) => void) {
        this.focusedJourneyDetailsListeners.push(listener)
        listener(this.focusedJourney?.journeyDetails ?? null)
    }
    removeJourneyDetailsListener(listener: (focused: JourneyDetailsWithTimes | null) => void) {
        this.focusedJourneyDetailsListeners = this.focusedJourneyDetailsListeners.filter(
            (l) => l !== listener,
        )
    }

    addWallTimetableListener(listener: (timetable: WallTimetableWithDates | null) => void) {
        this.wallTimetableListeners.push(listener)
        listener(this.selectedWallTimetable)
    }
    removeWallTimetableListener(listener: (timetable: WallTimetableWithDates | null) => void) {
        this.wallTimetableListeners = this.wallTimetableListeners.filter((l) => l !== listener)
    }

    addLineVersionSearchListener(
        listener: (lineVersions: SearchLineVersionWithDates[] | null) => void,
    ) {
        this.lineVersionSearchResultListeners.push(listener)
        listener(this.lineVersionSearchResult?.lineVersions ?? null)
    }
    removeLineVersionSearchListener(
        listener: (lineVersions: SearchLineVersionWithDates[] | null) => void,
    ) {
        this.lineVersionSearchResultListeners = this.lineVersionSearchResultListeners.filter(
            (l) => l !== listener,
        )
    }

    addAnimationPlayingListener(listener: (playing: boolean) => void) {
        this.animationPlayingListeners.push(listener)
        listener(this.animationPlaying)
    }
    removeAnimationPlayingListener(listener: (playing: boolean) => void) {
        this.animationPlayingListeners = this.animationPlayingListeners.filter(
            (l) => l !== listener,
        )
    }

    addAnimationSpeedListener(listener: (speed: number) => void) {
        this.animationSpeedListeners.push(listener)
        listener(this.animationSpeed)
    }
    removeAnimationSpeedListener(listener: (speed: number) => void) {
        this.animationSpeedListeners = this.animationSpeedListeners.filter(
            (l) => l !== listener,
        )
    }
}
