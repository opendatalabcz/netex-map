package cz.cvut.fit.gaierda1.domain.usecase.view

interface GetEncodedRoutesUseCase {
    data class EncodedRoute(
        val relationalId: Long,
        val pointSequence: String,
        val totalDistance: Double,
        val routeStopFractions: List<Double>,
    )
    fun getEncodedRoutes(routeIds: List<Long>): List<EncodedRoute>
}
