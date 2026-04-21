package cz.cvut.fit.gaierda1.data.http.routing.misc

import com.graphhopper.http.WebHelper
import com.graphhopper.util.PointList
import org.locationtech.jts.geom.Coordinate

class RouteConverter {
    companion object {
        fun convertEncodedPolylineToCoordinateList(encodedPolyline: String): List<Coordinate> {
            val pointList = WebHelper.decodePolyline(encodedPolyline, maxOf(2, encodedPolyline.length / 6), false)
            return (0 until pointList.size())
                .map { Coordinate(pointList.getLon(it), pointList.getLat(it)) }
        }

        fun convertCoordinateListToEncodedPolyline(coordinates: List<Coordinate>): String {
            val pointList = PointList(coordinates.size, false)
            coordinates.forEach { pointList.add(it.y, it.x) }
            return WebHelper.encodePolyline(pointList)
        }
    }
}

