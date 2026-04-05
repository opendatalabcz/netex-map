package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable

@Entity
@Table(name = "tariff_stop")
class TariffStop(
    @EmbeddedId
    val stopId: TariffStopId,

    @MapsId("lineVersionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_version_id", nullable = false)
    var lineVersion: LineVersion,

    @Column(nullable = true)
    val tariffZone: String?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    var stopPlace: Stop,
): Persistable<TariffStopId> {

    @Transient
    private var isNewEntity: Boolean = stopId.lineVersionId == null

    override fun getId(): TariffStopId = stopId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
