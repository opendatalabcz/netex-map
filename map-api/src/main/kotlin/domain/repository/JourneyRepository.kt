package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.LineId

interface JourneyRepository {
    fun saveIfAbsent(journey: Journey)
    fun saveAllIfAbsent(journeys: Iterable<Journey>)
    fun findById(lineId: LineId, validRange: DateRange, isDetour: Boolean, journeyId: JourneyId): Journey?
}
