package cz.cvut.fit.gaierda1.data.orm.repository.dto

data class TariffStopForSingleLineDto(
    val tariffOrder: Int,
    val tariffZone: String?,
    val stopId: Long,
)