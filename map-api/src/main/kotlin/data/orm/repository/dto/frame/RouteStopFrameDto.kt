package cz.cvut.fit.gaierda1.data.orm.repository.dto.frame

data class RouteStopFrameDto(
    val routeId: Long,
    val stopOrder: Int,
    val routeFraction: Double,
)
