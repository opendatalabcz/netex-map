package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.model.Journey

interface CalculateNextDayOperationUseCase {
    fun calculateFor(journey: Journey)
}
