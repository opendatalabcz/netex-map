package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.domain.port.OsmStopsServicePort

interface ImportPhysicalStopsFromOsmUseCase {
    fun importPhysicalStopsFromOsm(osmStopsServicePort: OsmStopsServicePort)
}
