package data.http.model

import com.fasterxml.jackson.annotation.JsonProperty

data class GHPostRequestBody(
    val points: List<List<Double>>,
    val profile: String,
    @JsonProperty("points_encoded")
    val pointsEncoded: Boolean,
    val instructions: Boolean,
    val elevation: Boolean,
    @JsonProperty("calc_points")
    val calcPoints: Boolean,
)
