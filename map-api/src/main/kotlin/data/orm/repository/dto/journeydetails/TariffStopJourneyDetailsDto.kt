package cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails

data class TariffStopJourneyDetailsDto(
    val tariffOrder: Int,
    val tariffZone: String?,
    val stopId: Long,
)
