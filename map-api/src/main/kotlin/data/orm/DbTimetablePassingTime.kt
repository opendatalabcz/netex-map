package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "timetable_passing_time")
class DbTimetablePassingTime(
    @EmbeddedId
    val id: DbTimetablePassingTimeId,

    @MapsId("journeyStopId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(
            name = "line_id",
            referencedColumnName = "line_id",
            nullable = false
        ),
        JoinColumn(
            name = "journey_pattern_line_local_id",
            referencedColumnName = "journey_pattern_line_local_id",
            nullable = false
        ),
        JoinColumn(
            name = "stop_order",
            referencedColumnName = "stop_order",
            nullable = false
        )
    )
    val stop: DbJourneyStop,

    @MapsId("journeyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "journey_id",
        nullable = false
    )
    val journey: DbJourney,

    @Column(nullable = true)
    val arrival: LocalTime?,

    @Column(nullable = true)
    val departure: LocalTime?,
){
}
