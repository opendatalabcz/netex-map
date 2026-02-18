package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.domain.model.Journey
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface JourneyViewPort {
    fun getJourneys(pageable: Pageable): Page<Journey> // TODO create domain page and pageable
}