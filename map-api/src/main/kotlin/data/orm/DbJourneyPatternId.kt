package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DbJourneyPatternId(
    val lineId: Long,

    @Column(name = "line_local_id")
    val lineLocalId: String,
){
}
