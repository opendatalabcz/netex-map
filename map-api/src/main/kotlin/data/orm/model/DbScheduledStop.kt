package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.domain.Persistable
import java.time.LocalTime

@Entity
@Table(name = "scheduled_stop")
class DbScheduledStop(
    @EmbeddedId
    val stopId: DbScheduledStopId,

    @Column(nullable = false)
    val name: String,

    @MapsId("journeyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    val journey: DbJourney,

    val arrival: LocalTime?,

    val departure: LocalTime?,
): Persistable<DbScheduledStopId> {

    @Transient
    private var isNewEntity: Boolean = (stopId.journeyId == null)

    override fun getId(): DbScheduledStopId = stopId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
