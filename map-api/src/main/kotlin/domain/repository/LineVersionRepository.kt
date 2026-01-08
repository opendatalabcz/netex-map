package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.model.LineVersion

interface LineVersionRepository {
    fun save(lineVersion: LineVersion)
    fun findById(lineId: LineId, validRange: DateRange): LineVersion?
}