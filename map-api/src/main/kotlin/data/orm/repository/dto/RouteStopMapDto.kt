package cz.cvut.fit.gaierda1.data.orm.repository.dto

data class RouteStopMapDto(
    val routeId: Long,
    val stopOrder: Int,
    val routeFraction: Double,
)
