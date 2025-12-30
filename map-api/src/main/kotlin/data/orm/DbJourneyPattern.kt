package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.locationtech.jts.geom.LineString

@Entity
@Table(name = "journey_pattern")
class DbJourneyPattern(
    @EmbeddedId
    val id: DbJourneyPatternId,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "journeyPattern")
    val stops: List<DbJourneyStop>,

    @Column(columnDefinition = "geometry(LineString,4326)", nullable = false)
    val route: LineString,

    @MapsId("lineId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    val line: DbLine,
) {
}
