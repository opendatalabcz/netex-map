package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Stop
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.StopWallDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StopJpaRepository: JpaRepository<Stop, Long> {
    @Query("SELECT s.relationalId FROM Stop s WHERE s.linePublicCode = :linePublicCode AND s.name = :name")
    fun findIdByLinePublicCodeAndName(linePublicCode: String, name: String): Optional<Long>

    @Query(nativeQuery = true, value = """
        SELECT
            s.relational_id,
            s.name,
            s.bistro,
            s.border_crossing,
            s.displays_for_visually_impaired,
            s.low_floor_access,
            s.park_and_ride_park,
            s.suitable_for_heavily_disabled,
            s.toilet,
            s.wheel_chair_access_toilet,
            s.other_transport_modes
        FROM stop s
        WHERE s.relational_id IN :stopIds
    """)
    fun findAllWallDtoByStopIds(stopIds: List<Long>): List<StopWallDto>
}
