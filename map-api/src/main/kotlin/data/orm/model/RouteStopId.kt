package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Embeddable

@Embeddable
data class RouteStopId(
    var routeId: Long?,
    val stopOrder: Int,
) {
}
