package cz.cvut.fit.gaierda1.data.orm.mapper

import cz.cvut.fit.gaierda1.domain.model.Point
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point as GeomPoint
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component

@Component
class GeometryMapper(
    private val geometryFactory: GeometryFactory = GeometryFactory(PrecisionModel(), 4326),
) {
    fun toDomain(point: GeomPoint): Point =
        Point(point.y, point.x)

    fun toDb(point: Point): GeomPoint =
        geometryFactory.createPoint(Coordinate(point.longitude, point.latitude))

    fun toDomain(lineString: LineString): List<Point> =
        lineString.coordinates.map { Point(longitude = it.x, latitude = it.y) }

    fun toDb(pointSequence: List<Point>): LineString =
        geometryFactory.createLineString(pointSequence.map { Coordinate(it.longitude, it.latitude) }.toTypedArray())
}
