package cz.cvut.fit.gaierda1.domain.model

@JvmInline
value class PhysicalStopId(val value: String)

data class PhysicalStop(
    val stopId: PhysicalStopId,
    val name: String,
    val position: Point,
    val tags: Map<String, String>,
){
}
