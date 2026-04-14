package cz.cvut.fit.gaierda1.data.orm.repository.dto.route

data class JourneyPatternStopRoutingDto(
    val lineVersionId: Long,
    val patternNumber: Int,
    val stopOrder: Int,
    val tariffOrder: Int,
)
