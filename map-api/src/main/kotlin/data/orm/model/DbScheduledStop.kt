package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "scheduled_stop")
class DbScheduledStop(
    @EmbeddedId
    val id: DbScheduledStopId,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "timetable_stop_id", nullable = false)
    val timetableStop: DbTimetableStop,

    @MapsId("journeyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    val journey: DbJourney,

    val arrival: LocalTime?,

    val departure: LocalTime?,
){
}
