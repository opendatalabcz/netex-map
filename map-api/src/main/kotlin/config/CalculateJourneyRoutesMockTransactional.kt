package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.Page
import cz.cvut.fit.gaierda1.domain.model.PageRequest
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import org.springframework.transaction.support.TransactionTemplate

class CalculateJourneyRoutesMockTransactional(
    private val journeyRepository: JourneyRepository,
    private val transactionTemplate: TransactionTemplate,
): CalculateJourneyRoutesMock(journeyRepository) {
    override fun calculateRoutes() {
        val pageSize = 30
        var currentPage: Page<Journey>? = null
        do { transactionTemplate.executeWithoutResult {
            currentPage = journeyRepository.findAllWithNullRoute(PageRequest(0, pageSize))
            for (journey in currentPage.content) {
                assignRoute(journey)
            }
            journeyRepository.saveAll(currentPage.content)
        } } while ((currentPage?.totalPages ?: 1) != 1)
    }
}