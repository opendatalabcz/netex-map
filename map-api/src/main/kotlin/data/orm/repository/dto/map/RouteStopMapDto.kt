package cz.cvut.fit.gaierda1.data.orm.repository.dto.map

data class RouteStopMapDto(
    val routeId: Long,
    val stopOrder: Int,
    val routeFraction: Double,
)
