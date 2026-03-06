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
        routeStops = route.routeStops.sortedBy { it.stopId.stopOrder }.map(::toDomain),
    )

    fun toDomain(routeStop: DbRouteStop): RouteStop = RouteStop(
        physicalStop = physicalStopRepositoryAdapter.toDomain(routeStop.physicalStop),
        pointSequenceIndex = routeStop.pointSequenceIndex,
    )

    fun toDb(route: Route, relationalId: Long?, physicalStops: List<DbPhysicalStop>): DbRoute {
        val routeStops = mutableListOf<DbRouteStop>()
        val dbRoute = DbRoute(
            relationalId = relationalId,
            externalId = route.routeId.value,
            pointSequence = geometryAdapter.toDb(route.pointSequence),
            routeStops = routeStops,
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
    )

    fun findOrMap(route: Route, physicalStopsSupplier: () -> List<DbPhysicalStop>): DbRoute {
        val optionalSaved = routeJpaRepository.findByExternalId(route.routeId.value)
        return optionalSaved.orElseGet { toDb(route, null, physicalStopsSupplier()) }
    }

    fun saveDb(route: DbRoute) {
        routeJpaRepository.save(route)
        routeStopJpaRepository.saveAll(route.routeStops)
    }

    fun saveAllDb(routes: Iterable<DbRoute>) {
        routeJpaRepository.saveAll(routes)
        routeStopJpaRepository.saveAll(routes.flatMap { it.routeStops })
    }

    fun findSaveMapping(route: Route): FindSaveSingleMapping {
        var toSavePhysicalStopMappings = emptyList<DbPhysicalStop>()
        val mappedRoute = findOrMap(route, {
            val physicalStopsMapping = physicalStopRepositoryAdapter.findSaveMappings(route.routeStops.map { it.physicalStop })
            toSavePhysicalStopMappings = physicalStopsMapping.toSavePhysicalStops
            physicalStopsMapping.physicalStops
        })
        return FindSaveSingleMapping(mappedRoute, mappedRoute.relationalId == null, toSavePhysicalStopMappings)
    }

    private val routeComparator = compareBy<Route> { it.routeId.value }

    private fun findSaveMappingsImpl(routes: Iterable<Route>, result: Boolean): Triple<List<DbRoute>?, List<DbRoute>, List<DbPhysicalStop>> {
        val uniqueRoutes = sortedSetOf(comparator = routeComparator)
        uniqueRoutes.addAll(routes)
        val routeStopsCountPrefixSum = uniqueRoutes.map { it.routeStops.size }.runningReduce(Int::plus)
        val mappedPhysicalStops = physicalStopRepositoryAdapter
            .findSaveMappings(uniqueRoutes.flatMap { it.routeStops.map { it.physicalStop } })

        val mappedUniqueRoutes = uniqueRoutes.mapIndexed { idx, route ->
            val prefixSum = routeStopsCountPrefixSum[idx]
            findOrMap(route, { mappedPhysicalStops.physicalStops.subList(prefixSum - route.routeStops.size, prefixSum) })
        }
        return Triple(if (result) {
            routes.map { domainRoute -> mappedUniqueRoutes.find { dbRoute -> domainRoute.routeId.value == dbRoute.externalId }!! }
        } else null, mappedUniqueRoutes.filter { it.relationalId == null }, mappedPhysicalStops.toSavePhysicalStops)
    }

    fun findSaveMappings(routes: Iterable<Route>): FindSaveMultipleMapping {
        val res = findSaveMappingsImpl(routes, result = true)
        return FindSaveMultipleMapping(res.first!!, res.second, res.third)
    }

    override fun saveIfAbsent(route: Route) {
        val mapping = findSaveMapping(route)
        if (mapping.save) {
            if (mapping.toSavePhysicalStops.isNotEmpty()) {
                physicalStopRepositoryAdapter.saveAllDb(mapping.toSavePhysicalStops)
            }
            saveDb(mapping.route)
        }
    }

    override fun saveAllIfAbsent(routes: Iterable<Route>) {
        val res = findSaveMappingsImpl(routes, result = false)
        if (res.second.isNotEmpty()) {
            if (res.third.isNotEmpty()) {
                physicalStopRepositoryAdapter.saveAllDb(res.third)
            }
            saveAllDb(res.second)
        }
    }

    override fun findById(id: RouteId): Route? {
        return routeJpaRepository
            .findByExternalId(id.value)
            .map(::toDomain)
            .orElse(null)
    }

    data class FindSaveSingleMapping(
        val route: DbRoute,
        val save: Boolean,
        val toSavePhysicalStops: List<DbPhysicalStop>,
    )
    data class FindSaveMultipleMapping(
        val routes: List<DbRoute>,
        val toSaveRoutes: List<DbRoute>,
        val toSavePhysicalStops: List<DbPhysicalStop>,
    )
}
