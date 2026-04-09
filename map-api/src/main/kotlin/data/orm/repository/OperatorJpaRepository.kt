package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Operator
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatorDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OperatorJpaRepository: JpaRepository<Operator, Long> {
    @Query("SELECT o.relationalId FROM Operator o WHERE o.publicCode = :publicCode")
    fun findIdByPublicCode(publicCode: String): Optional<Long>

    @Query(nativeQuery = true, value = """
        SELECT
            o.relational_id,
            o.public_code,
            o.legal_name,
            o.phone,
            o.email,
            o.url,
            o.address_line
        FROM operator o
        WHERE o.relational_id = :operatorId
    """)
    fun findDtoByOperatorId(operatorId: Long): Optional<OperatorDto>

    @Query(nativeQuery = true, value = """
        SELECT
            o.relational_id,
            o.public_code,
            o.legal_name,
            o.phone,
            o.email,
            o.url,
            o.address_line
        FROM operator o
        WHERE o.relational_id IN :operatorIds
    """)
    fun findAllDtoByOperatorIds(operatorIds: List<Long>): List<OperatorDto>
}
