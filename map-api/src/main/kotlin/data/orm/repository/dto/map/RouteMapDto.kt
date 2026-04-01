package cz.cvut.fit.gaierda1.data.orm.repository.dto.map

data class RouteMapDto(
    val relationalId: Long,
    val pointSequence: ByteArray,
    val totalDistance: Double,
)
