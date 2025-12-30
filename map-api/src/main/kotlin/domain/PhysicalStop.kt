package cz.cvut.fit.gaierda1.domain

@JvmInline
value class PhysicalStopId(val value: String)

data class PhysicalStop(
    val stopId: PhysicalStopId,
    val name: String,
    val position: Point,
    val tags: Map<String, String> = emptyMap(),
){
}
