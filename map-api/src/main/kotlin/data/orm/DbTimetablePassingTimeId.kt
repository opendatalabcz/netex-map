package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class DbTimetablePassingTimeId(
    @Embedded
    val journeyStopId: DbJourneyStopId,

    @Column(name = "journey_id")
    val journeyId: Long,
){
}
