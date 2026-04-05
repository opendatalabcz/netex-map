package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Stop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StopJpaRepository: JpaRepository<Stop, Long> {
    @Query("SELECT s.relationalId FROM Stop s WHERE s.linePublicCode = :linePublicCode AND s.name = :name")
    fun findIdByLinePublicCodeAndName(linePublicCode: String, name: String): Optional<Long>
}
