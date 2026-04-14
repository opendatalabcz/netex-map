package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.JourneyPattern
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.route.JourneyPatternRoutingDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyPatternWallDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JourneyPatternJpaRepository: JpaRepository<JourneyPattern, JourneyPatternId> {
    @Modifying
    @Query(nativeQuery = true, value = """
        UPDATE journey_pattern SET route_id = :routeId
        WHERE line_version_id = :lineVersionId AND pattern_number = :patternNumber
    """)
    fun setRouteForJourneyPatternById(lineVersionId: Long, patternNumber: Int, routeId: Long)

    @Query(nativeQuery = true , value = """
        SELECT jp.pattern_number, jp.direction, jp.route_id
        FROM journey_pattern jp
        WHERE jp.line_version_id = :lineVersionId
    """)
    fun findAllWallDtoByLineVersionId(lineVersionId: Long): List<JourneyPatternWallDto>

    @Query(nativeQuery = true, value = """
        SELECT jp.line_version_id, jp.pattern_number
        FROM journey_pattern jp
        WHERE jp.route_id IS NULL
        ORDER BY jp.line_version_id, jp.pattern_number
    """)
    fun findAllRoutingDtoWithNullRoute(): List<JourneyPatternRoutingDto>
}
