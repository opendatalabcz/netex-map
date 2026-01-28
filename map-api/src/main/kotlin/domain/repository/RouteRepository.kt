package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.Route
import cz.cvut.fit.gaierda1.domain.model.RouteId

interface RouteRepository {
    fun saveIfAbsent(route: Route)
    fun saveAllIfAbsent(routes: Iterable<Route>)
    fun findById(id: RouteId): Route?
}
