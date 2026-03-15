package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStop
import cz.cvut.fit.gaierda1.data.orm.model.ScheduledStopId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.ScheduledStopMapDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ScheduledStopJpaRepository: JpaRepository<ScheduledStop, ScheduledStopId> {
    @Query(nativeQuery = true, value = """
        SELECT ss.journey_id, ss.stop_order, ss.arrival, ss.departure
        FROM scheduled_stop ss
        WHERE journey_id IN :journeyIds
    """)
    fun findAllMapDtoByJourneyId(journeyIds: List<Long>): List<ScheduledStopMapDto>
}
