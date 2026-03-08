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
import java.time.LocalTime

@Entity
@Table(name = "scheduled_stop")
class ScheduledStop(
    @EmbeddedId
    val stopId: ScheduledStopId,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val stopOnRequest: Boolean,

    @MapsId("journeyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    val journey: Journey,

    val arrival: LocalTime?,

    val departure: LocalTime?,
): Persistable<ScheduledStopId> {

    @Transient
    private var isNewEntity: Boolean = (stopId.journeyId == null)

    override fun getId(): ScheduledStopId = stopId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
