package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.RouteStop
import cz.cvut.fit.gaierda1.data.orm.model.RouteStopId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.frame.RouteStopFrameDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RouteStopJpaRepository: JpaRepository<RouteStop, RouteStopId> {
    @Query(nativeQuery = true, value = """
        SELECT rs.route_id, rs.stop_order, rs.route_fraction
        FROM route_stop rs
        WHERE route_id IN :routeIds
    """)
    fun findAllFrameDtoByRouteIds(routeIds: List<Long>): List<RouteStopFrameDto>
}
