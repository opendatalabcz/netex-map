package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.model.LineType
import org.springframework.stereotype.Component

@Component
class LevelOfDetail(
    private val levelOfDetailProperties: LevelOfDetailProperties,
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

    override fun getVisibleLineTypes(zoomLevel: Int): List<String> {
        val visibleLineTypes = mutableListOf(
            LineType.DOMESTIC_INTER_REGIONAL,
            LineType.DOMESTIC_INTRA_REGIONAL,
            LineType.DOMESTIC_LONG_DISTANCE,
            LineType.INTERNATIONAL_INCLUDING_CABOTAGE,
            LineType.INTERNATIONAL_EXCLUDING_CABOTAGE,
        )
        if (zoomLevel >= levelOfDetailProperties.cityLinesZoomThreshold) {
            visibleLineTypes.add(LineType.URBAN)
            visibleLineTypes.add(LineType.URBAN_SUBURBAN)
        }
        return visibleLineTypes.map(LineType::shortCode)
    }
}
