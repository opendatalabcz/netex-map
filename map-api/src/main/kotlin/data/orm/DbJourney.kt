package cz.cvut.fit.gaierda1.data.orm

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
import jakarta.persistence.Table

@Entity
@Table(name = "journey")
class DbJourney(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val relationalId: Long,

    @Column(nullable = false, unique = true)
    val externalId: String,

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
    val pattern: DbJourneyPattern,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "journey")
    val schedule: List<DbTimetablePassingTime>,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operating_period_id", nullable = false)
    val operatingPeriod: DbOperatingPeriod,
) {
}