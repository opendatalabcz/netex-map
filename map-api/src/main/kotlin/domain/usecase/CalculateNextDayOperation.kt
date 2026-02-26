package cz.cvut.fit.gaierda1.domain.usecase

import cz.cvut.fit.gaierda1.domain.model.Journey

class CalculateNextDayOperation: CalculateNextDayOperationUseCase {
    override fun calculateFor(journey: Journey) {
        var previousDepartureTime = journey.schedule.first().run { arrival ?: departure!! }
        journey.schedule.forEachIndexed { idx, stop ->
            val currentArrivalTime = stop.arrival ?: stop.departure!!
            if (previousDepartureTime > currentArrivalTime) {
                journey.nextDayFirstStopIndex = idx
                return
            }
            val currentDepartureTime = stop.departure ?: stop.arrival!!
            if (currentArrivalTime > currentDepartureTime) {
                if (idx + 1 < journey.schedule.size) {
                    journey.nextDayFirstStopIndex = idx + 1
                }
                return
            }
            previousDepartureTime = currentDepartureTime
        }
    }
}
