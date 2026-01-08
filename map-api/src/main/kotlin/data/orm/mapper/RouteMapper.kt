package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId
import cz.cvut.fit.gaierda1.domain.model.RouteStop
import org.springframework.stereotype.Component

@Component
class RouteMapper(
    private val geometryMapper: GeometryMapper,
    private val physicalStopMapper: PhysicalStopMapper,
    private val routeJpaRepository: RouteJpaRepository,
) {
    fun toDomain(route: DbRoute): Route = Route(
        routeId = RouteId(route.externalId),
        pointSequence = geometryMapper.toDomain(route.pointSequence),
        routeStops = route.routeStops.map(::routeStopToDomain),
    )

    private fun routeStopToDomain(routeStop: DbRouteStop): RouteStop = RouteStop(
        physicalStop = physicalStopMapper.toDomain(routeStop.physicalStop),
        pointSequenceIndex = routeStop.id.pointSequenceIndex,
    )

    fun toDb(route: Route): DbRoute {
        val saved = routeJpaRepository.findByExternalId(route.routeId.value)
        val routeStops = mutableListOf<DbRouteStop>()

        val dbRoute = DbRoute(
            relationalId = saved.map { it.relationalId }.orElse(null),
            externalId = route.routeId.value,
            pointSequence = geometryMapper.toDb(route.pointSequence),
            routeStops = routeStops,
        )
        routeStops.addAll(route.routeStops.map { routeStopToDb(it, dbRoute) })

        return dbRoute
    }

    private fun routeStopToDb(routeStop: RouteStop, route: DbRoute): DbRouteStop = DbRouteStop(
        id = DbRouteStopId(route.relationalId, routeStop.pointSequenceIndex),
        physicalStop = physicalStopMapper.toDb(routeStop.physicalStop),
        route = route,
    )
}