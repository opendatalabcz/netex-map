package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney

class CalculateNextDayOperationData: CalculateNextDayOperationDataUseCase {
    override fun calculateFor(journey: DbJourney) {
        var previousTime = journey.schedule.first().run { departure ?: arrival!! }
        journey.schedule.forEachIndexed { idx, stop ->
            val currentDepartureTime = stop.departure ?: stop.arrival!!
            if (previousTime > currentDepartureTime) {
                journey.nextDayFirstStopIndex = idx
                return
            }
            previousTime = currentDepartureTime
        }
    }
}