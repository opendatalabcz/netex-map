package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.Convert
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
    val relationalId: Long,

    @Column(nullable = false)
    val externalId: String,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    val position: Point,

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = MapToJsonConverter::class)
    val tags: Map<String, String> = emptyMap()
) {
}
