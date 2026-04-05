package cz.cvut.fit.gaierda1.data.orm.repository.dto

data class OperatorDto(
    val relationalId: Long,
    val publicCode: String,
    val legalName: String,
    val phone: String,
    val email: String,
    val url: String,
    val addressLine: String,
)
