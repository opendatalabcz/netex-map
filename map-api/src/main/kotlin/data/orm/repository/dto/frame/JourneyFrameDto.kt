package cz.cvut.fit.gaierda1.data.orm.repository.dto.frame

data class JourneyFrameDto(
    val relationalId: Long,
    val lineVersionId: Long,
    val routeId: Long,
    val nextDayFirstStopIndex: Int?,
    val timezone: String,
)
