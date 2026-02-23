package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.Page
import cz.cvut.fit.gaierda1.domain.model.PageRequest
import cz.cvut.fit.gaierda1.domain.port.JourneyViewPort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository

class ViewJourneys(private val journeyRepository: JourneyRepository): JourneyViewPort {
    override fun getJourneys(pageRequest: PageRequest): Page<Journey> {
        return journeyRepository.findAll(pageRequest)
    }
}
