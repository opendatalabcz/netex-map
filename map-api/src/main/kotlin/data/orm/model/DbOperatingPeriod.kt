package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "operating_period")
class DbOperatingPeriod(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var relationalId: Long?,

    @Column(nullable = false)
    val timezone: ZoneId,

    @Column(nullable = false)
    val fromDate: LocalDateTime,

    @Column(nullable = false)
    val toDate: LocalDateTime,

    @Column(columnDefinition = "boolean[]", nullable = false)
    val validDays: List<Boolean>,
) {
}
