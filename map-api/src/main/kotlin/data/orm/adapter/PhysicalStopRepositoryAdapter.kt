package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.PhysicalStopMapper
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import cz.cvut.fit.gaierda1.domain.repository.PhysicalStopRepository
import org.springframework.stereotype.Component

@Component
class PhysicalStopRepositoryAdapter(
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val physicalStopMapper: PhysicalStopMapper,
): PhysicalStopRepository {
    override fun save(physicalStop: PhysicalStop) {
        physicalStopJpaRepository.save(physicalStopMapper.toDb(physicalStop))
    }

    override fun findById(id: PhysicalStopId): PhysicalStop? {
        return physicalStopJpaRepository.findByExternalId(id.value)
            .map(physicalStopMapper::toDomain)
            .orElse(null)
    }
}
