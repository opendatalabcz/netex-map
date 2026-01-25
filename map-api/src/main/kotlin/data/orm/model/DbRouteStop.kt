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
@Table(name = "route_stop")
class DbRouteStop(
    @EmbeddedId
    val stopId: DbRouteStopId,

    @MapsId("routeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    val route: DbRoute,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "physical_stop_id", nullable = false)
    val physicalStop: DbPhysicalStop,

    @Column(nullable = false)
    val pointSequenceIndex: Int,
): Persistable<DbRouteStopId> {

    @Transient
    private var isNewEntity: Boolean = (stopId.routeId == null)

    override fun getId(): DbRouteStopId = stopId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
