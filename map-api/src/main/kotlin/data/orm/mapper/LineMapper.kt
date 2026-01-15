package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.data.orm.model.DbLine
import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId
import org.springframework.stereotype.Component

@Component
class LineMapper {
    fun toDomain(line: DbLine): Line = Line(
        lineId = LineId(line.externalId),
        publicCode = line.publicCode,
    )

    fun toDb(line: Line): DbLine = DbLine(
        relationalId = null,
        externalId = line.lineId.value,
        publicCode = line.publicCode,
    )
}
