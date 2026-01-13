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
import jakarta.persistence.Table

@Entity
@Table(name = "journey")
class DbJourney(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var relationalId: Long?,

    @Column(nullable = false)
    val externalId: String,

    @Column(nullable = false)
    val journeyPatternId: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "line_version_id", nullable = false)
    val lineVersion: DbLineVersion,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    var route: DbRoute?,

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "journey")
    val schedule: List<DbScheduledStop>,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "journey_operating_period",
        joinColumns = [JoinColumn(name = "journey_id")],
        inverseJoinColumns = [JoinColumn(name = "operating_period_id")]
    )
    val operatingPeriods: List<DbOperatingPeriod>,
) {
}
