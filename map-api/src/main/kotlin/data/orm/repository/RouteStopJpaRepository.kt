package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.RouteStopMapDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RouteStopJpaRepository: JpaRepository<RouteStop, RouteStopId> {
    @Query(nativeQuery = true, value = """
        SELECT rs.route_id, rs.stop_order, rs.point_sequence_index, rs.distance_to_next_stop
        FROM route_stop rs
        WHERE route_id IN :routeIds
    """)
    fun findAllDtoByRouteIds(routeIds: List<Long>): List<RouteStopMapDto>
}
