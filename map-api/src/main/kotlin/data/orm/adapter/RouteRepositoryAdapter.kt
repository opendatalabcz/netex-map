package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId
import cz.cvut.fit.gaierda1.domain.model.RouteStop
import cz.cvut.fit.gaierda1.domain.repository.RouteRepository
import org.springframework.stereotype.Component

@Component
class RouteRepositoryAdapter(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val physicalStopRepositoryAdapter: PhysicalStopRepositoryAdapter,
    private val geometryAdapter: GeometryAdapter,
): RouteRepository {
    fun toDomain(route: DbRoute): Route = Route(
        routeId = RouteId(route.externalId),
        pointSequence = geometryAdapter.toDomain(route.pointSequence),
        routeStops = route.routeStops.sortedBy { it.id.stopOrder }.map(::toDomain),
    )

    fun toDomain(routeStop: DbRouteStop): RouteStop = RouteStop(
        physicalStop = physicalStopRepositoryAdapter.toDomain(routeStop.physicalStop),
        pointSequenceIndex = routeStop.pointSequenceIndex,
    )

    fun toDb(route: Route, relationalId: Long?): DbRoute {
        val routeStops = mutableListOf<DbRouteStop>()
        val dbRoute = DbRoute(
            relationalId = relationalId,
            externalId = route.routeId.value,
            pointSequence = geometryAdapter.toDb(route.pointSequence),
            routeStops = routeStops,
        )
        routeStops.addAll(route.routeStops.mapIndexed { index, stop -> toDb(stop, dbRoute, index) })
        return dbRoute
    }

    fun toDb(routeStop: RouteStop, route: DbRoute, order: Int): DbRouteStop = DbRouteStop(
        id = DbRouteStopId(route.relationalId, order),
        physicalStop = physicalStopRepositoryAdapter.findSaveMapping(routeStop.physicalStop),
        route = route,
        pointSequenceIndex = routeStop.pointSequenceIndex,
    )

    fun findSaveMapping(route: Route): DbRoute {
        val optionalSaved = routeJpaRepository.findByExternalId(route.routeId.value)
        if (optionalSaved.isPresent) {
            return optionalSaved.get()
        }
        val saved = routeJpaRepository.save(toDb(route, null))
        for (routeStop in saved.routeStops) {
            routeStop.id.routeId = saved.relationalId
        }
        routeStopJpaRepository.saveAll(saved.routeStops)
        return saved
    }

    override fun save(route: Route) {
        findSaveMapping(route)
    }

    override fun findById(id: RouteId): Route? {
        return routeJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }
}
