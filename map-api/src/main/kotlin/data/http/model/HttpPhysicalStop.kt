package cz.cvut.fit.gaierda1.data.http.model

data class HttpPhysicalStop(
    val stopId: String,
    val name: String,
    val position: Array<Double>,
    val tags: Map<String, String>,
) {
}
