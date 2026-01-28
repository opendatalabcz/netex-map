package cz.cvut.fit.gaierda1.data.orm.adapter.pedantic

import cz.cvut.fit.gaierda1.data.orm.adapter.GeometryAdapter
import cz.cvut.fit.gaierda1.data.orm.adapter.PhysicalStopRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.adapter.RouteRepositoryAdapter
import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStop
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Route
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RouteRepositoryPedanticAdapter(
    private val routeJpaRepository: RouteJpaRepository,
    routeStopJpaRepository: RouteStopJpaRepository,
    physicalStopRepositoryAdapter: PhysicalStopRepositoryAdapter,
    geometryAdapter: GeometryAdapter,
): RouteRepositoryAdapter(routeJpaRepository, routeStopJpaRepository, physicalStopRepositoryAdapter, geometryAdapter) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun findOrMap(route: Route, physicalStopsSupplier: () -> List<DbPhysicalStop>): DbRoute {
        val optionalSaved = routeJpaRepository.findByExternalId(route.routeId.value)
        if (optionalSaved.isPresent) {
            val saved = optionalSaved.get()
            compareRouteStop(route, saved)
            return saved
        }
        return toDb(route, null, physicalStopsSupplier())
    }

    private fun compareRouteStop(route: Route, savedRoute: DbRoute) {
        val sortedSavedStops = savedRoute.routeStops.sortedBy { it.stopId.stopOrder }
        if (route.routeStops.size != sortedSavedStops.size) logDifference("number of route stops", sortedSavedStops.size, route.routeStops.size, savedRoute)
        for ((new, old) in route.routeStops.zip(sortedSavedStops)) {
            if (new.pointSequenceIndex != old.pointSequenceIndex) {
                logDifference("point sequence index", old.pointSequenceIndex, new.pointSequenceIndex, savedRoute, old)
            }
            if (new.physicalStop.stopId.value != old.physicalStop.externalId) {
                logDifference("physical stop id", old.physicalStop.externalId, new.physicalStop.stopId.value, savedRoute, old)
            }
        }
    }

    private fun logDifference(fieldName: String, old: Any?, new: Any?, contextRoute: DbRoute, contextStop: DbRouteStop) {
        logDifference("stop [${contextStop.stopId.stopOrder}]: $fieldName", old, new, contextRoute)
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
