package cz.cvut.fit.gaierda1.presentation.model

data class HttpPhysicalStop(
    val relationalId: Long?,
    val externalId: String,
    val name: String,
    val position: Array<Double>,
    val tags: Map<String, String>,
) {
}
