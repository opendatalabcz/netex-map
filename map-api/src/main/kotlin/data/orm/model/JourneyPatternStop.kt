package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable

@Entity
@Table(name = "journey_pattern_stop")
class JourneyPatternStop(
    @EmbeddedId
    val stopId: JourneyPatternStopId,

    @MapsId("journeyPatternId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "line_version_id", referencedColumnName = "line_version_id", nullable = false),
        JoinColumn(name = "pattern_number", referencedColumnName = "pattern_number", nullable = false),
    )
    var journeyPattern: JourneyPattern,

    @Column(nullable = false)
    val distanceToNextStop: Double,

    @Column(name = "tariff_order", nullable = false)
    val tariffOrder: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "line_version_id", referencedColumnName = "line_version_id", nullable = false, insertable = false, updatable = false),
        JoinColumn(name = "tariff_order", referencedColumnName = "tariff_order", nullable = false, insertable = false, updatable = false),
    )
    var tariffStop: TariffStop,

    @Column(nullable = false)
    val forBoarding: Boolean,

    @Column(nullable = false)
    val forAlighting: Boolean,

    @Column(nullable = false)
    val requiresOrdering: Boolean,

    @Column(nullable = false)
    val stopOnRequest: Boolean,
): Persistable<JourneyPatternStopId> {

    @Transient
    private var isNewEntity: Boolean = stopId.journeyPatternId.lineVersionId == null

    override fun getId(): JourneyPatternStopId = stopId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
