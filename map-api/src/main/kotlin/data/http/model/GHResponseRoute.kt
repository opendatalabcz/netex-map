package data.http.model

import com.fasterxml.jackson.annotation.JsonProperty

data class GHResponseRoute(
    val distance: Double,
    val time: Int,
    val ascend: Double,
    val descend: Double,
    val points: String,
    @JsonProperty("points_encoded")
    val pointsEncoded: Boolean,
    @JsonProperty("snapped_waypoints")
    val snappedWaypoints: String,
    val bbox: List<Double>, // minLon, minLat, maxLon, maxLat
//    val instructions: Not supported,
)
