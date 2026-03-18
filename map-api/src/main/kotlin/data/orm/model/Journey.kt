package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.BatchSize

@Entity
@Table(
    name = "journey",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["line_version_id", "external_id"])
    ]
)
class Journey(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journey_seq_gen")
    @SequenceGenerator(name = "journey_seq_gen", sequenceName = "journey_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false)
    val externalId: String,

    @Column(nullable = false)
    val journeyPatternId: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "line_version_id", nullable = false)
    val lineVersion: LineVersion,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    var route: Route?,

    @BatchSize(size = 30)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "journey")
    val schedule: List<ScheduledStop>,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operating_period_id", nullable = false)
    val operatingPeriods: OperatingPeriod,

    var nextDayFirstStopIndex: Int?,
) {
}
