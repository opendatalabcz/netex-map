package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.RouteFrameDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.RouteStopFrameDto
import org.springframework.stereotype.Component

@Component
class GetEncodedRoutes(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
): GetEncodedRoutesUseCase {
    override fun getEncodedRoutes(routeIds: List<Long>): List<GetEncodedRoutesUseCase.EncodedRoute> {
        val rawRoutes = if (routeIds.isEmpty()) emptyList() else routeJpaRepository.findAllFrameDtoByRouteId(routeIds)
        val routeStopFractions = routeStopJpaRepository.findAllFrameDtoByRouteIds(rawRoutes.map(RouteFrameDto::relationalId))
            .groupBy(RouteStopFrameDto::routeId)
            .mapValues { (_, routeStops) -> routeStops
                .sortedBy(RouteStopFrameDto::stopOrder)
                .map(RouteStopFrameDto::routeFraction)
            }

        return rawRoutes.map { route ->
            GetEncodedRoutesUseCase.EncodedRoute(
                relationalId = route.relationalId,
                pointSequence = route.pointSequence,
                totalDistance = route.totalDistance,
                routeStopFractions = routeStopFractions[route.relationalId]!!,
            )
        }
    }
}
