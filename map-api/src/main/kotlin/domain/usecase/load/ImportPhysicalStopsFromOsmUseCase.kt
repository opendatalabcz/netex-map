package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.domain.port.OsmParserPort
import java.io.File

interface ImportPhysicalStopsFromOsmUseCase {
    fun importPhysicalStopsFromOsm(
        osmFile: File,
        osmParserPort: OsmParserPort,
    )
}
