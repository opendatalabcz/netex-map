package cz.cvut.fit.gaierda1.data.http.stops.model

data class OverpassRelationMemberElement(
    val type: String,
    val ref: Long,
    val role: String,
)
