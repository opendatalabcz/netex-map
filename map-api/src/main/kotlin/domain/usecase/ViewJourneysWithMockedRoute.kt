package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.port.JourneyViewPort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class ViewJourneysWithMockedRoute(
    private val journeyRepository: JourneyRepository,
    private val assignRouteMock: AssignRouteMock,
): JourneyViewPort {
    override fun getJourneys(pageable: Pageable): Page<Journey> {
        return journeyRepository.getPage(pageable).map(assignRouteMock::assignRoute)
    }
}
