package cz.cvut.fit.gaierda1.data.orm.adapter

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component

@Component
class GeometryAdapter(
    private val geometryFactory: GeometryFactory = GeometryFactory(PrecisionModel(), 4326),
) {
    fun toDomain(point: Point): cz.cvut.fit.gaierda1.domain.model.Point =
        cz.cvut.fit.gaierda1.domain.model.Point(longitude = point.x, latitude = point.y)

    fun toDb(point: cz.cvut.fit.gaierda1.domain.model.Point): Point =
        geometryFactory.createPoint(Coordinate(point.longitude, point.latitude))

    fun toDomain(lineString: LineString): List<cz.cvut.fit.gaierda1.domain.model.Point> =
        lineString.coordinates.map { cz.cvut.fit.gaierda1.domain.model.Point(longitude = it.x, latitude = it.y) }

    fun toDb(pointSequence: List<cz.cvut.fit.gaierda1.domain.model.Point>): LineString =
        geometryFactory.createLineString(pointSequence.map { Coordinate(it.longitude, it.latitude) }.toTypedArray())
}