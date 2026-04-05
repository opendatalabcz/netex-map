package cz.cvut.fit.gaierda1.data.orm.model

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
@Table(name = "within_region_transport_ban")
class WithinRegionTransportBan(
    @EmbeddedId
    val banId: WithinRegionTransportBanId,

    @MapsId("journeyPatternStopId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "line_version_id", referencedColumnName = "line_version_id", nullable = false),
        JoinColumn(name = "pattern_number", referencedColumnName = "pattern_number", nullable = false),
        JoinColumn(name = "stop_order", referencedColumnName = "stop_order", nullable = false),
    )
    var journeyPatternStop: JourneyPatternStop,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "line_version_id", referencedColumnName = "line_version_id", nullable = false, insertable = false, updatable = false),
        JoinColumn(name = "pattern_number", referencedColumnName = "pattern_number", nullable = false, insertable = false, updatable = false),
    )
    var journeyPattern: JourneyPattern,
): Persistable<WithinRegionTransportBanId> {

    @Transient
    private var isNewEntity: Boolean = banId.journeyPatternStopId.journeyPatternId.lineVersionId == null

    override fun getId(): WithinRegionTransportBanId = banId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
