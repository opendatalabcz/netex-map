package cz.cvut.fit.gaierda1.data.orm.repository.dto

data class JourneyMapDto(
    val relationalId: Long,
    val lineVersionId: Long,
    val routeId: Long,
    val nextDayFirstStopIndex: Int?,
    val timezone: String,
)
