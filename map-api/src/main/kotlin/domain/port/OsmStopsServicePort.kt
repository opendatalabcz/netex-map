package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop

interface OsmStopsServicePort {
    fun getPhysicalStops(): List<PhysicalStop>
}
