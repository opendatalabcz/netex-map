package cz.cvut.fit.gaierda1.data.orm.adapter.pedantic

import cz.cvut.fit.gaierda1.data.orm.adapter.GeometryAdapter
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId
import cz.cvut.fit.gaierda1.domain.model.RouteStop
import cz.cvut.fit.gaierda1.domain.repository.RouteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RouteRepositoryPedanticAdapter(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val physicalStopRepositoryPedanticAdapter: PhysicalStopRepositoryPedanticAdapter,
    private val geometryAdapter: GeometryAdapter,
): RouteRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun toDomain(route: DbRoute): Route = Route(
        routeId = RouteId(route.externalId),
        pointSequence = geometryAdapter.toDomain(route.pointSequence),
        routeStops = route.routeStops.sortedBy { it.id.stopOrder }.map(::toDomain),
    )

    fun toDomain(routeStop: DbRouteStop): RouteStop = RouteStop(
        physicalStop = physicalStopRepositoryPedanticAdapter.toDomain(routeStop.physicalStop),
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
        physicalStop = physicalStopRepositoryPedanticAdapter.findSaveMapping(routeStop.physicalStop),
        route = route,
        pointSequenceIndex = routeStop.pointSequenceIndex,
    )

    fun findSaveMapping(route: Route): DbRoute {
        val optionalSaved = routeJpaRepository.findByExternalId(route.routeId.value)
        if (optionalSaved.isPresent) {
            return findSaveExistingMapping(route, optionalSaved.get())
        }
        val saved = routeJpaRepository.save(toDb(route, null))
        for (routeStop in saved.routeStops) {
            routeStop.id.routeId = saved.relationalId
        }
        routeStopJpaRepository.saveAll(saved.routeStops)
        return saved
    }

    private fun findSaveExistingMapping(route: Route, savedRoute: DbRoute): DbRoute {
        var differ = false
        val toDelete = mutableListOf<DbRouteStop>()
        val sortedSavedStops = savedRoute.routeStops.sortedBy { it.id.stopOrder }
        for ((new, old) in route.routeStops.zipWithFill(sortedSavedStops)) {
            if (new != null && old != null) {
                if (new.pointSequenceIndex != old.pointSequenceIndex) {
                    differ = true
                    logDifference("point sequence index", old.pointSequenceIndex, new.pointSequenceIndex, savedRoute, old)
                }
                if (new.physicalStop.stopId.value != old.physicalStop.externalId) {
                    differ = true
                    logDifference("physical stop id", old.physicalStop.externalId, new.physicalStop.stopId.value, savedRoute, old)
                } else if (
                    new.physicalStop.name != old.physicalStop.name
                    || new.physicalStop.position != geometryAdapter.toDomain(old.physicalStop.position)
                    || new.physicalStop.tags != old.physicalStop.tags
                ) {
                    differ = true
                    // the difference will be logged in the findSaveMapping of the physicalStop
                }
            } else if (new != null) {
                differ = true
            } else {
                differ = true
                toDelete.add(old!!)
            }
        }
        if (!differ) return savedRoute

        logDifference("number of route stops", sortedSavedStops.size, route.routeStops.size, savedRoute)
        routeStopJpaRepository.deleteAll(toDelete)
        val mapped = routeJpaRepository.save(toDb(route, savedRoute.relationalId))
        routeStopJpaRepository.saveAll(mapped.routeStops)
        return mapped
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

    private fun logDifference(fieldName: String, old: Any?, new: Any?, contextRoute: DbRoute, contextStop: DbRouteStop) {
        logDifference("stop [${contextStop.id.stopOrder}]: $fieldName", old, new, contextRoute)
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, context: DbRoute) {
        log.warn(
            "Route {}: {} changed from {} to {}",
            context.externalId,
            fieldName,
            old,
            new
        )
    }
}
