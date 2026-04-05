package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Embeddable

@Embeddable
data class WithinRegionTransportBanId(
    val journeyPatternStopId: JourneyPatternStopId,
    val banGroupNumber: Int,
) {
}
