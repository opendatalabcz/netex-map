package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney

interface CalculateNextDayOperationDataUseCase {
    fun calculateFor(journey: DbJourney)
}