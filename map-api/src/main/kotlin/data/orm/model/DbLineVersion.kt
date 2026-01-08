package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "line_version")
class DbLineVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var relationalId: Long?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "line_id", nullable = false)
    val line: DbLine,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val shortName: String,

    @Column(nullable = false)
    val transportMode: String,

    @Column(nullable = false)
    val isDetour: Boolean,

    @Column(nullable = false)
    val validFrom: LocalDateTime,

    @Column(nullable = false)
    val validTo: LocalDateTime,

    @Column(nullable = false)
    val timezone: ZoneId,
){
}
