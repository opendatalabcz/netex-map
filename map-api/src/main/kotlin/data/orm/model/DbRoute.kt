package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.locationtech.jts.geom.LineString

@Entity
@Table(name = "route")
class DbRoute(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "route_seq_gen")
    @SequenceGenerator(name = "route_seq_gen", sequenceName = "route_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false, unique = true)
    val externalId: String,

    @Column(columnDefinition = "geography(LineString,4326)", nullable = false)
    val pointSequence: LineString,

    @OneToMany(mappedBy = "route", fetch = FetchType.EAGER)
    val routeStops: List<DbRouteStop>
) {
}
