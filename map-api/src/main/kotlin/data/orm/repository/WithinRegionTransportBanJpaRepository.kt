package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.WithinRegionTransportBan
import cz.cvut.fit.gaierda1.data.orm.model.WithinRegionTransportBanId
import cz.cvut.fit.gaierda1.data.orm.repository.dto.journeydetails.TransportBanJourneyDetailsDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.WithinRegionTransportBanWallDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface WithinRegionTransportBanJpaRepository: JpaRepository<WithinRegionTransportBan, WithinRegionTransportBanId> {
    @Query(nativeQuery = true, value = """
        SELECT tb.pattern_number, tb.stop_order, tb.ban_group_number
        FROM within_region_transport_ban tb
        WHERE tb.line_version_id = :lineVersionId
    """)
    fun findAllWallDtoByLineVersionId(lineVersionId: Long): List<WithinRegionTransportBanWallDto>

    @Query(nativeQuery = true, value = """
        SELECT tb.stop_order, tb.ban_group_number
        FROM within_region_transport_ban tb
        WHERE tb.line_version_id = :lineVersionId AND tb.pattern_number = :patternNumber
    """)
    fun findAllJourneyDetailsDtoByLineVersionIdAndPatternNumber(lineVersionId: Long, patternNumber: Int): List<TransportBanJourneyDetailsDto>
}
