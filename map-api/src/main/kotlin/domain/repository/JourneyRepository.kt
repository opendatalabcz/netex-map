package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.DateTimeRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.JourneyPatternId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.model.Page
import cz.cvut.fit.gaierda1.domain.model.PageRequest
import cz.cvut.fit.gaierda1.domain.model.Route

interface JourneyRepository {
    data class SeRouteByLineVersionAndJourneyPatternTriplet(
        val lineVersion: LineVersion,
        val journeyPatternId: JourneyPatternId,
        val route: Route,
    )
    fun saveIfAbsent(journey: Journey)
    fun saveAllIfAbsent(journeys: Iterable<Journey>)
    fun save(journey: Journey)
    fun saveAll(journeys: Iterable<Journey>)
    fun findById(lineId: LineId, validRange: DateTimeRange, isDetour: Boolean, journeyId: JourneyId): Journey?
    fun findAll(pageRequest: PageRequest): Page<Journey>
    fun findAllWithNullRoute(pageRequest: PageRequest): Page<Journey>
    fun findAllWithDistinctJourneyPatternWithNullRoute(pageRequest: PageRequest): Page<Journey>
    fun setRouteForAllByLineVersionAndJourneyPattern(triplets: List<SeRouteByLineVersionAndJourneyPatternTriplet>)
}
