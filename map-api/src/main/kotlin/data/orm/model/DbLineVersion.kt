package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(
    name = "line_version",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["external_id", "valid_from", "valid_to", "timezone", "is_detour"])
    ]
)
class DbLineVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "line_version_seq_gen")
    @SequenceGenerator(name = "line_version_seq_gen", sequenceName = "line_version_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false)
    val externalId: String,

    @Column(nullable = false)
    val publicCode: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val shortName: String,

    @Column(nullable = false)
    val transportMode: String,

    @Column(nullable = false)
    val isDetour: Boolean,

    @Column(nullable = false)
    val validFrom: LocalDateTime,

    @Column(nullable = false)
    val validTo: LocalDateTime,

    @Column(nullable = false)
    val timezone: ZoneId,
){
}
