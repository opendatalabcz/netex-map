package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "timetable_stop")
class DbTimetableStop(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var relationalId: Long?,

    @Column(unique = true)
    val externalId: String,

    @Column(nullable = false)
    val name: String,
) {
}
