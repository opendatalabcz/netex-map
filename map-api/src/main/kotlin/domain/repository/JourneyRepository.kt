package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.DateTimeRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.LineId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface JourneyRepository {
    fun saveIfAbsent(journey: Journey)
    fun saveAllIfAbsent(journeys: Iterable<Journey>)
    fun findById(lineId: LineId, validRange: DateTimeRange, isDetour: Boolean, journeyId: JourneyId): Journey?
    fun getPage(pageable: Pageable): Page<Journey>
}
