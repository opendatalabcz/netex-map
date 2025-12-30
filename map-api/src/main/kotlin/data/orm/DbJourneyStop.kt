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

@Entity
@Table(name = "journey_stop")
class DbJourneyStop(
    @EmbeddedId
    val id: DbJourneyStopId,

    @MapsId("journeyPatternId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(
            name = "line_id",
            referencedColumnName = "line_id",
            nullable = false
        ),
        JoinColumn(
            name = "journey_pattern_line_local_id",
            referencedColumnName = "line_local_id",
            nullable = false
        )
    )
    val journeyPattern: DbJourneyPattern,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_table_stop_relational_id", nullable = false)
    val stop: DbTimetableStop,

    @Column(nullable = true)
    var routeIndex: Int?,
) {
}
