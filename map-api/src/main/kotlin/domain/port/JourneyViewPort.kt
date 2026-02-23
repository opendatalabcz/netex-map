package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.Page
import cz.cvut.fit.gaierda1.domain.model.PageRequest

interface JourneyViewPort {
    fun getJourneys(pageRequest: PageRequest): Page<Journey>
}