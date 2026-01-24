package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "route_stop")
class DbRouteStop(
    @EmbeddedId
    val id: DbRouteStopId,

    @MapsId("routeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    val route: DbRoute,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "physical_stop_id", nullable = false)
    val physicalStop: DbPhysicalStop,

    @Column(nullable = false)
    val pointSequenceIndex: Int,
) {
}
