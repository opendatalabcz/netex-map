package cz.cvut.fit.gaierda1.data.http.stops.model

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null,
    val members: List<OverpassRelationMemberElement>? = null,
    val center: OverpassRelationCenter? = null,
)
