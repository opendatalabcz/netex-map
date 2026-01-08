package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.LineVersionMapper
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import org.springframework.stereotype.Component

@Component
class LineVersionRepositoryAdapter(
    private val lineVersionJpaRepository: LineVersionJpaRepository,
    private val lineVersionMapper: LineVersionMapper,
): LineVersionRepository {
    override fun save(lineVersion: LineVersion) {
        lineVersionJpaRepository.save(lineVersionMapper.toDb(lineVersion))
    }

    override fun findById(lineId: LineId, validRange: DateRange): LineVersion? {
        return lineVersionJpaRepository.findByLineIdAndValidRange(
            lineExternalId = lineId.value,
            validFrom = validRange.from,
            validTo = validRange.to,
            timezone = validRange.timezone,
        ).map(lineVersionMapper::toDomain)
            .orElse(null)
    }
}
