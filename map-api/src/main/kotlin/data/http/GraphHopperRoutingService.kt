package cz.cvut.fit.gaierda1.data.http

import cz.cvut.fit.gaierda1.data.http.misc.RouteConverter
import cz.cvut.fit.gaierda1.data.http.model.GHBadRequestResponseBody
import cz.cvut.fit.gaierda1.domain.port.RoutingServicePort
import data.http.model.GHPostRequestBody
import data.http.model.GHResponseBody
import org.locationtech.jts.geom.Coordinate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class GraphHopperRoutingService(
    @Value($$"${routing.service.url}") baseUrl: String,
): RoutingServicePort {
    private val log = LoggerFactory.getLogger(GraphHopperRoutingService::class.java)
    private val webClient = WebClient.builder().baseUrl(baseUrl).build()

    override fun getRouteForPoints(points: List<Coordinate>): RoutingServicePort.RoutingResponse? {
        val requestBody = GHPostRequestBody(
            points = points.map { listOf(it.x, it.y) },
            profile = "car",
            pointsEncoded = true,
            instructions = false,
            elevation = false,
            calcPoints = true,
        )
        val response = try {
            webClient.post()
                .uri("/route")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(GHResponseBody::class.java)
                .blockOptional()
                .orElseGet {
                    error("No response from GraphHopper. Request body: $requestBody")
                }
        } catch (e: WebClientResponseException.BadRequest) {
            log.debug("Error while retrieving route: ${e.getResponseBodyAs(GHBadRequestResponseBody::class.java)?.message}")
            return null
        }
        val responseRoute = response.paths.firstOrNull() ?: return null
        return RoutingServicePort.RoutingResponse(
            route = RouteConverter.convertEncodedPolylineToCoordinateList(responseRoute.points),
            distance = responseRoute.distance,
            waypoints = RouteConverter.convertEncodedPolylineToCoordinateList(responseRoute.snappedWaypoints),
        )
    }
}
