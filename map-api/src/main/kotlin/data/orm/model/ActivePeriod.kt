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
import java.time.OffsetDateTime

@Entity
@Table(name = "active_period")
class ActivePeriod(
    @EmbeddedId
    val periodId: ActivePeriodId,

    @MapsId("lineVersionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_version_id", nullable = false)
    val lineVersion: LineVersion,

    @Column(nullable = false)
    val toDate: OffsetDateTime,
): Persistable<ActivePeriodId> {
    @Transient
    private var isNewEntity: Boolean = (periodId.lineVersionId == null)

    override fun getId(): ActivePeriodId = periodId

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}