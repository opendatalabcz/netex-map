package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.model.Journey

class CalculateNextDayOperation: CalculateNextDayOperationUseCase {
    override fun calculateFor(journey: Journey) {
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