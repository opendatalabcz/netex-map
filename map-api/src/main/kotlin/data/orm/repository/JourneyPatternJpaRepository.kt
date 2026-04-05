package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.JourneyPattern
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternId
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
}
