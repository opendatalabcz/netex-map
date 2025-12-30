package cz.cvut.fit.gaierda1.data.orm

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "line")
class DbLine(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val relationalId: Long,

    @Column(nullable = false, unique = true)
    val externalId: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val shortName: String,

    @Column(nullable = false)
    val publicCode: String,

    @Column(nullable = false)
    val transportMode: String,
){
}
