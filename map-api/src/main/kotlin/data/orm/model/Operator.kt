package cz.cvut.fit.gaierda1.data.orm.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "operator")
class Operator(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "operator_seq_gen")
    @SequenceGenerator(name = "operator_seq_gen", sequenceName = "operator_seq", allocationSize = 20)
    var relationalId: Long?,

    @Column(nullable = false, unique = true)
    val publicCode: String,

    @Column(nullable = false)
    val legalName: String,

    @Column(nullable = false)
    val phone: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val url: String,

    @Column(nullable = false)
    val addressLine: String,
) {
}
