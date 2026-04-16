package cz.cvut.fit.gaierda1.domain.usecase.view

interface LevelOfDetailUseCase {
    fun getMinRouteLength(zoomLevel: Int): Double
    fun getVisibleLineTypes(zoomLevel: Int): List<String>
}
