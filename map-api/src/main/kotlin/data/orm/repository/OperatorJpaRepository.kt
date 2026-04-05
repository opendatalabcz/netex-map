package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Operator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OperatorJpaRepository: JpaRepository<Operator, Long> {
    @Query("SELECT o.relationalId FROM Operator o WHERE o.publicCode = :publicCode")
    fun findIdByPublicCode(publicCode: String): Optional<Long>
}
