package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbLine
import cz.cvut.fit.gaierda1.data.orm.repository.LineJpaRepository
import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId
import org.springframework.stereotype.Component

@Component
class LineMapper(
    private val lineJpaRepository: LineJpaRepository,
) {
    fun toDomain(line: DbLine): Line = Line(
        lineId = LineId(line.externalId),
        publicCode = line.publicCode,
    )

    fun toDb(line: Line): DbLine {
        val saved = lineJpaRepository.findByExternalId(line.lineId.value)
        return DbLine(
            relationalId = saved.map { it.relationalId }.orElse(null),
            externalId = line.lineId.value,
            publicCode = line.publicCode,
        )
    }
}
