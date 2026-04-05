package cz.cvut.fit.gaierda1.domain.usecase.view

import org.springframework.stereotype.Component

@Component
class LevelOfDetail(
    levelOfDetailProperties: LevelOfDetailProperties,
): LevelOfDetailUseCase {
    private val minRouteLengthList =
        levelOfDetailProperties.minRouteLength.toList()

    override fun getMinRouteLength(zoomLevel: Int): Double {
        val firstLod = minRouteLengthList.firstOrNull() ?: return 0.0
        if (zoomLevel <= firstLod.first) return firstLod.second
        var previousLod = firstLod
        for (lod in minRouteLengthList.drop(1)) {
            if (zoomLevel < lod.first) return previousLod.second
            previousLod = lod
        }
        return previousLod.second
    }
}
