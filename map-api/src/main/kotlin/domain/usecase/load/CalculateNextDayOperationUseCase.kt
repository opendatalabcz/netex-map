package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.Journey

interface CalculateNextDayOperationUseCase {
    fun calculateFor(journey: Journey)
}
