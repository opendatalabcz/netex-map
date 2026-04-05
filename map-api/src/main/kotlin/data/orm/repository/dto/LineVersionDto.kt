package cz.cvut.fit.gaierda1.data.orm.repository.dto

data class LineVersionDto(
    val relationalId: Long,
    val publicCode: String,
    val name: String,
    val shortName: String,
    val transportMode: String,
    val lineType: String,
    val isDetour: Boolean,
    val operatorId: Long,
)