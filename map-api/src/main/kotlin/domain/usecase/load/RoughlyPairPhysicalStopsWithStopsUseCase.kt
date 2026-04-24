package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.StopPositionEnrichmentDto

interface RoughlyPairPhysicalStopsWithStopsUseCase {
    data class StopPair(
        val physicalStop: PhysicalStop,
        val stop: StopPositionEnrichmentDto,
    )
    fun pairPhysicalStops(
        normalizeStopNameUseCase: NormalizeStopNameUseCase,
    ): List<StopPair>
}
