package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import org.springframework.stereotype.Component

@Component
class PhysicalStopMapper(
    private val geometryMapper: GeometryMapper,
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
) {
    fun toDomain(physicalStop: DbPhysicalStop): PhysicalStop = PhysicalStop(
        stopId = PhysicalStopId(physicalStop.externalId),
        name = physicalStop.name,
        position = geometryMapper.toDomain(physicalStop.position),
        tags = physicalStop.tags
    )

    fun toDb(physicalStop: PhysicalStop): DbPhysicalStop {
        val saved = physicalStopJpaRepository.findByExternalId(physicalStop.stopId.value)
        return DbPhysicalStop(
            relationalId = saved.map { it.relationalId }.orElse(null),
            externalId = physicalStop.stopId.value,
            name = physicalStop.name,
            position = geometryMapper.toDb(physicalStop.position),
            tags = physicalStop.tags
        )
    }
}
