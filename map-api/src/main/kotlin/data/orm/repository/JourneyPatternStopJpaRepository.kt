package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStop
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPatternStopId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JourneyPatternStopJpaRepository: JpaRepository<JourneyPatternStop, JourneyPatternStopId> {
}
