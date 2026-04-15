package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Route
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.RouteFrameDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RouteJpaRepository: JpaRepository<Route, Long> {
    @Query(nativeQuery = true, value = """
        SELECT r.relational_id,
           ST_AsEncodedPolyline(r.point_sequence::geometry) as polyline,
           r.total_distance
        FROM route r
        WHERE r.relational_id IN :routeIds
    """)
    fun findAllFrameDtoByRouteId(routeIds: List<Long>): List<RouteFrameDto>

    fun findAllByExternalId(externalId: String): List<Route>
}
