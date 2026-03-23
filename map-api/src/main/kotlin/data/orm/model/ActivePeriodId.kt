package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Embeddable
import java.time.OffsetDateTime

@Embeddable
data class ActivePeriodId(
    var lineVersionId: Long?,
    val fromDate: OffsetDateTime,
) {
}
