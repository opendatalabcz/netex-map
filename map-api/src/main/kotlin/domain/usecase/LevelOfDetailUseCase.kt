package cz.cvut.fit.gaierda1.domain.usecase

interface LevelOfDetailUseCase {
    fun getMinRouteLength(zoomLevel: Int): Double
}