package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.DbTimetableStop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TimetableStopJpaRepository: JpaRepository<DbTimetableStop, Long> {
    fun findByExternalId(externalId: String): Optional<DbTimetableStop>
}
