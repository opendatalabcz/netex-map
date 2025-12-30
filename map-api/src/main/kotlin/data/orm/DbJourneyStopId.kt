package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DbJourneyStopId(
    val journeyPatternId: DbJourneyPatternId,

    @Column(name = "stop_order")
    val stopOrder: Int,
) {
}
