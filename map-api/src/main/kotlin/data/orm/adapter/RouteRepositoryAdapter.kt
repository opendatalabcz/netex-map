package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRoute
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStop
import cz.cvut.fit.gaierda1.data.orm.model.DbRouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId
import cz.cvut.fit.gaierda1.domain.model.RouteStop
import cz.cvut.fit.gaierda1.domain.repository.RouteRepository
import cz.cvut.fit.gaierda1.measuring.Measurer
import org.springframework.stereotype.Component

@Component
open class RouteRepositoryAdapter(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val physicalStopRepositoryAdapter: PhysicalStopRepositoryAdapter,
    private val geometryAdapter: GeometryAdapter,
): RouteRepository {
    fun toDomain(route: DbRoute): Route = Route(
        routeId = RouteId(route.externalId),
        pointSequence = geometryAdapter.toDomain(route.pointSequence),
        routeStops = route.routeStops.sortedBy { it.stopId.stopOrder }.map(::toDomain),
        totalDistance = route.totalDistance,
    )

    fun toDomain(routeStop: DbRouteStop): RouteStop = RouteStop(
        physicalStop = physicalStopRepositoryAdapter.toDomain(routeStop.physicalStop),
        pointSequenceIndex = routeStop.pointSequenceIndex,
        distanceToNextStop = routeStop.distanceToNextStop,
    )

    fun toDb(route: Route, relationalId: Long?, physicalStops: List<DbPhysicalStop>): DbRoute {
        val routeStops = mutableListOf<DbRouteStop>()
        val dbRoute = DbRoute(
            relationalId = relationalId,
            externalId = route.routeId.value,
            pointSequence = geometryAdapter.toDb(route.pointSequence),
            routeStops = routeStops,
            totalDistance = route.totalDistance,
        )
        routeStops.addAll(
            route.routeStops
                .zip(physicalStops)
                .mapIndexed { index, stopPair -> toDb(stopPair.first, dbRoute, index, stopPair.second) }
        )
        return dbRoute
    }

    fun toDb(routeStop: RouteStop, route: DbRoute, order: Int, physicalStop: DbPhysicalStop): DbRouteStop = DbRouteStop(
        stopId = DbRouteStopId(route.relationalId, order),
        physicalStop = physicalStop,
        route = route,
        pointSequenceIndex = routeStop.pointSequenceIndex,
        distanceToNextStop = routeStop.distanceToNextStop,
    )

    fun findOrMap(route: Route, physicalStopsSupplier: () -> List<DbPhysicalStop>): DbRoute {
        ++Measurer.searchedRoutes
        val optionalSaved = Measurer.addToDbFind { routeJpaRepository.findByExternalId(route.routeId.value) }
        return optionalSaved.orElseGet { toDb(route, null, physicalStopsSupplier()) }
    }

    fun saveDb(route: DbRoute) {
        ++Measurer.savedRoutes
        Measurer.savedRouteStops += route.routeStops.size
        Measurer.addToDbSave {
            routeJpaRepository.save(route)
            routeStopJpaRepository.saveAll(route.routeStops)
        }
    }

    fun saveAllDb(routes: Iterable<DbRoute>) {
        Measurer.savedRoutes += routes.count()
        Measurer.savedRouteStops += routes.sumOf { it.routeStops.size }
        Measurer.addToDbSave {
            routeJpaRepository.saveAll(routes)
            routeStopJpaRepository.saveAll(routes.flatMap { it.routeStops })
        }
    }

    fun findSaveMapping(route: Route): DbRoute {
        val mappedRoute = findOrMap(route, {
            physicalStopRepositoryAdapter.findSaveMappings(route.routeStops.map { it.physicalStop })
        })
        if (mappedRoute.relationalId == null) saveDb(mappedRoute)
        return mappedRoute
    }

    private val routeComparator = compareBy<Route> { it.routeId.value }

    private fun findSaveMappingsImpl(routes: Iterable<Route>, result: Boolean): List<DbRoute>? {
        val uniqueRoutes = sortedSetOf(comparator = routeComparator)
        uniqueRoutes.addAll(routes)
        val routeStopsCountPrefixSum = uniqueRoutes.map { it.routeStops.size }.runningReduce(Int::plus)
        val mappedPhysicalStops = physicalStopRepositoryAdapter
            .findSaveMappings(uniqueRoutes.flatMap { it.routeStops.map { it.physicalStop } })

        val mappedUniqueRoutes = uniqueRoutes.mapIndexed { idx, route ->
            val prefixSum = routeStopsCountPrefixSum[idx]
            findOrMap(route, { mappedPhysicalStops.subList(prefixSum - route.routeStops.size, prefixSum) })
        }
        saveAllDb(mappedUniqueRoutes.filter { it.relationalId == null })
        return if (result) {
            routes.map { domainRoute -> mappedUniqueRoutes.find { dbRoute -> domainRoute.routeId.value == dbRoute.externalId }!! }
        } else null
    }

    fun findSaveMappings(routes: Iterable<Route>): List<DbRoute> {
        return findSaveMappingsImpl(routes, result = true)!!
    }

    override fun saveIfAbsent(route: Route) {
        findSaveMapping(route)
    }

    override fun saveAllIfAbsent(routes: Iterable<Route>) {
        findSaveMappingsImpl(routes, result = false)
    }

    override fun findById(id: RouteId): Route? {
        return routeJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }
}
