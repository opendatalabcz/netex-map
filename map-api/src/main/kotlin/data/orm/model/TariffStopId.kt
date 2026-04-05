package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class TariffStopId(
    var lineVersionId: Long?,
    @Column(name = "tariff_order")
    val tariffOrder: Int,
) {
}
