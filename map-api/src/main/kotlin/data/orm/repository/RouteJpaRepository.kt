package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.repository.dto.map.RouteMapDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RouteJpaRepository: JpaRepository<Route, Long> {
    @Query(nativeQuery = true, value = """
        SELECT r.relational_id,
           ST_AsBinary(r.point_sequence) as wkb_geometry,
           r.total_distance
        FROM route r
        WHERE r.relational_id IN :routeIds
    """)
    fun findAllMapDtoByRouteId(routeIds: List<Long>): List<RouteMapDto>

    fun findAllByExternalId(externalId: String): List<Route>
}
