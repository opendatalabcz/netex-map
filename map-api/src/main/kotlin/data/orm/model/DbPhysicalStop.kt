package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point

@Entity
@Table(name = "physical_stop")
class DbPhysicalStop(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var relationalId: Long?,

    @Column(nullable = false)
    val externalId: String,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    val position: Point,

    @Column(columnDefinition = "jsonb", nullable = false)
    val tags: Map<String, String>
) {
}
