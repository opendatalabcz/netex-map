package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.BatchSize
import java.time.LocalTime
import java.time.ZoneId

@Entity
@Table(
    name = "journey",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["line_version_id", "journey_number"])
    ]
)
class Journey(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journey_seq_gen")
    @SequenceGenerator(name = "journey_seq_gen", sequenceName = "journey_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false)
    val journeyNumber: String,

    @BatchSize(size = 30)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "journey")
    var schedule: List<ScheduledStop>,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operating_period_id", nullable = false)
    var operatingPeriod: OperatingPeriod,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "line_version_id", referencedColumnName = "line_version_id", nullable = false),
        JoinColumn(name = "pattern_number", referencedColumnName = "pattern_number", nullable = false),
    )
    var journeyPattern: JourneyPattern,

    @Column(nullable = false)
    val requiresOrdering: Boolean,

    @Column(nullable = false)
    val baggageStorage: Boolean,

    @Column(nullable = false)
    val cyclesAllowed: Boolean,

    @Column(nullable = false)
    val lowFloorAccess: Boolean,

    @Column(nullable = false)
    val reservationCompulsory: Boolean,

    @Column(nullable = false)
    val reservationPossible: Boolean,

    @Column(nullable = false)
    val snacksOnBoard: Boolean,

    @Column(nullable = false)
    val unaccompaniedMinorAssistance: Boolean,

    var nextDayFirstStopIndex: Int?,

    @Column(nullable = false)
    var beginTime: LocalTime,

    @Column(nullable = false)
    var endTime: LocalTime,

    @Column(nullable = false)
    val timezone: ZoneId,
) {
}
