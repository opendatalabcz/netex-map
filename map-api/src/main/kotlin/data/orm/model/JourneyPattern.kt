package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.springframework.data.domain.Persistable

@Entity
@Table(name = "journey_pattern")
class JourneyPattern(
    @EmbeddedId
    val patternId: JourneyPatternId,

    @MapsId("lineVersionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_version_id", nullable = false)
    var lineVersion: LineVersion,

    @Column(nullable = false)
    val direction: JourneyDirectionType,

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "journeyPattern", fetch = FetchType.EAGER)
    var patternStops: List<JourneyPatternStop>,

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "journeyPattern", fetch = FetchType.EAGER)
    var transportBans: List<WithinRegionTransportBan>,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = true)
    var route: Route?,
): Persistable<JourneyPatternId> {

    @Transient
    private var isNewEntity: Boolean = patternId.lineVersionId == null

    override fun getId(): JourneyPatternId = patternId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
