package cz.cvut.fit.gaierda1.data.orm.repository.dto.wall

data class TariffStopWallDto(
    val tariffOrder: Int,
    val tariffZone: String?,
    val stopId: Long,
)
