package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.DateTimeRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.Page
import cz.cvut.fit.gaierda1.domain.model.PageRequest

interface JourneyRepository {
    fun saveIfAbsent(journey: Journey)
    fun saveAllIfAbsent(journeys: Iterable<Journey>)
    fun save(journey: Journey)
    fun saveAll(journeys: Iterable<Journey>)
    fun findById(lineId: LineId, validRange: DateTimeRange, isDetour: Boolean, journeyId: JourneyId): Journey?
    fun findAll(pageRequest: PageRequest): Page<Journey>
    fun findAllWithNullRoute(pageRequest: PageRequest): Page<Journey>
}
