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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.BatchSize
import java.time.OffsetDateTime

@Entity
@Table(
    name = "line_version",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["public_code", "valid_from", "valid_to", "is_detour"])
    ]
)
class LineVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "line_version_seq_gen")
    @SequenceGenerator(name = "line_version_seq_gen", sequenceName = "line_version_seq", allocationSize = 20)
    var relationalId: Long?,

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
    val validFrom: OffsetDateTime,

    @Column(nullable = false)
    val validTo: OffsetDateTime,

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "lineVersion", fetch = FetchType.EAGER)
    val activePeriods: List<ActivePeriod>,
){
}
