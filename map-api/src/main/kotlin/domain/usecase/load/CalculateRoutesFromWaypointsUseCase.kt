package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.model.Route
import org.locationtech.jts.geom.Coordinate
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.math.cos
import kotlin.math.sqrt

interface CalculateRoutesFromWaypointsUseCase {
    fun calculateRouteFromWaypoints(
        waypoints: List<PhysicalStop>,
        cache: RouteCalculationCache? = null,
    ): Route?

    companion object {
        const val EARTH_RADIUS = 6_371_000.0

        fun distanceBetweenPoints(a: Coordinate, b: Coordinate): Double {
            val cosLat = cos(a.y * Math.PI / 180)
            val dLonRad = (b.x - a.x) * Math.PI / 180 * cosLat
            val dLatRad = (b.y - a.y) * Math.PI / 180
            return EARTH_RADIUS * sqrt(dLatRad * dLatRad + dLonRad * dLonRad)
        }

        fun externalIdFromWaypoints(waypoints: List<PhysicalStop>): String {
            return Base64.encode(
                MessageDigest.getInstance("SHA-256")
                    .digest(waypoints.joinToString("|") { it.externalId }.toByteArray())
            )
        }
    }
}
