package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PhysicalStopJpaRepository: JpaRepository<PhysicalStop, Long> {
    fun findByExternalId(externalId: String): Optional<PhysicalStop>

    @Query("SELECT ps FROM PhysicalStop ps WHERE ps.externalId IN :externalIds")
    fun findAllByExternalIds(externalIds: List<String>): List<PhysicalStop>
}
