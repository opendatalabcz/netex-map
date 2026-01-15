package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbPhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.model.PhysicalStopId
import org.springframework.stereotype.Component

@Component
class PhysicalStopMapper(
    private val geometryMapper: GeometryMapper,
) {
    fun toDomain(physicalStop: DbPhysicalStop): PhysicalStop = PhysicalStop(
        stopId = PhysicalStopId(physicalStop.externalId),
        name = physicalStop.name,
        position = geometryMapper.toDomain(physicalStop.position),
        tags = physicalStop.tags
    )

    fun toDb(physicalStop: PhysicalStop): DbPhysicalStop = DbPhysicalStop(
        relationalId = null,
        externalId = physicalStop.stopId.value,
        name = physicalStop.name,
        position = geometryMapper.toDb(physicalStop.position),
        tags = physicalStop.tags
    )
}
