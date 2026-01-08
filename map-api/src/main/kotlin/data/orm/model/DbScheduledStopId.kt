package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Embeddable

@Embeddable
data class DbScheduledStopId(
    var journeyId: Long?,
    val stopOrder: Int,
){
}
