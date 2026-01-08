package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.Line
import cz.cvut.fit.gaierda1.domain.model.LineId

interface LineRepository {
    fun save(line: Line)
    fun findById(id: LineId): Line?
}
