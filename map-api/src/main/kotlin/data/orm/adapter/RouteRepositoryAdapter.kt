package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.RouteMapper
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId
import cz.cvut.fit.gaierda1.domain.repository.RouteRepository
import org.springframework.stereotype.Component

@Component
class RouteRepositoryAdapter(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val routeMapper: RouteMapper,
): RouteRepository {
    override fun save(route: Route) {
        val dbRoute = routeMapper.toDb(route)
        routeJpaRepository.save(dbRoute)
        dbRoute.routeStops.forEach {
            it.id.routeId = dbRoute.relationalId
            routeStopJpaRepository.save(it)
        }
    }

    override fun findById(id: RouteId): Route? {
        return routeJpaRepository.findByExternalId(id.value)
            .map(routeMapper::toDomain)
            .orElse(null)
    }
}
