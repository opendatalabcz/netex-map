package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.TariffStop
import cz.cvut.fit.gaierda1.data.orm.model.TariffStopId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.TariffStopWallDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TariffStopJpaRepository: JpaRepository<TariffStop, TariffStopId> {
    @Query(nativeQuery = true, value = """
        SELECT ts.tariff_order, ts.tariff_zone, ts.stop_id
        FROM tariff_stop ts
        WHERE ts.line_version_id = :lineVersionId
    """)
    fun findAllWallDtoByLineVersionId(lineVersionId: Long): List<TariffStopWallDto>
}
