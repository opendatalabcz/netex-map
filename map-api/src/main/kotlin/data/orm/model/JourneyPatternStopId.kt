package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class JourneyPatternStopId(
    val journeyPatternId: JourneyPatternId,
    @Column(name = "stop_order")
    val stopOrder: Int,
) {
}
