package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import java.io.File

interface OsmParserPort {
    fun parseOsmFile(osmFile: File): List<PhysicalStop>
}
