package cz.cvut.fit.gaierda1.domain.usecase.data

import cz.cvut.fit.gaierda1.data.orm.model.DbJourney

class CalculateNextDayOperationData: CalculateNextDayOperationDataUseCase {
    override fun calculateFor(journey: DbJourney) {
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
