package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStop
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStopId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.JourneyPatternStopWallDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JourneyPatternStopJpaRepository: JpaRepository<JourneyPatternStop, JourneyPatternStopId> {
    @Query(nativeQuery = true, value = """
        SELECT
            jps.pattern_number,
            jps.stop_order,
            jps.tariff_order,
            jps.distance_to_next_stop,
            jps.for_boarding,
            jps.for_alighting,
            jps.requires_ordering,
            jps.stop_on_request
        FROM journey_pattern_stop jps
        WHERE jps.line_version_id = :lineVersionId
    """)
    fun findAllWallDtoByLineVersionId(lineVersionId: Long): List<JourneyPatternStopWallDto>
}
