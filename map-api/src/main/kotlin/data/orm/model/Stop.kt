package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "stop",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["line_public_code", "name"]),
    ],
)
class Stop(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_seq_gen")
    @SequenceGenerator(name = "stop_seq_gen", sequenceName = "stop_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val linePublicCode: String,

    @Column(nullable = false)
    val bistro: Boolean,

    @Column(nullable = false)
    val borderCrossing: Boolean,

    @Column(nullable = false)
    val displaysForVisuallyImpaired: Boolean,

    @Column(nullable = false)
    val lowFloorAccess: Boolean,

    @Column(nullable = false)
    val parkAndRidePark: Boolean,

    @Column(nullable = false)
    val suitableForHeavilyDisabled: Boolean,

    @Column(nullable = false)
    val toilet: Boolean,

    @Column(nullable = false)
    val wheelChairAccessToilet: Boolean,

    @Column(nullable = true)
    val otherTransportModes: String?,
) {
}
