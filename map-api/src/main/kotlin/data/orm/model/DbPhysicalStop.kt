package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point

@Entity
@Table(name = "physical_stop")
class DbPhysicalStop(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "physical_stop_seq_gen")
    @SequenceGenerator(name = "physical_stop_seq_gen", sequenceName = "physical_stop_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false, unique = true)
    val externalId: String,

    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    val position: Point,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    val tags: Map<String, String>
) {
}
