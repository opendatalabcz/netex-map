package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class JourneyPatternId(
    var lineVersionId: Long?,
    @Column(name = "pattern_number")
    val patternNumber: Int,
) {
}
