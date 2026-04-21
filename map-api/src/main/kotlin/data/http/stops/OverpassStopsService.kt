package cz.cvut.fit.gaierda1.data.http.stops

import cz.cvut.fit.gaierda1.data.http.stops.model.OverpassResponseBody
import cz.cvut.fit.gaierda1.data.http.stops.model.OverpassElement
import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.port.OsmStopsServicePort
import cz.cvut.fit.gaierda1.domain.port.ServiceUnavailableException
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class OverpassStopsService(
    @Value($$"${stops.service.url}") baseUrl: String,
    @Value($$"${stops.service.health-check}") private val healthCheck: String,
): OsmStopsServicePort {
    companion object {
        private const val ALL_STOPS_QUERY = """
            [out:json][timeout:180];
            (
                node["public_transport"~"stop_position|platform"];
                node["highway"="bus_stop"];
                node["railway"~"halt|station|stop|tram_stop"];
                rel["public_transport"="stop_area"];
            );
            out qt center;
        """
    }

    private val log = LoggerFactory.getLogger(OverpassStopsService::class.java)
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024)
        }
        .build()
    private val geometryFactory = GeometryFactory()
        private val objectMapper = ObjectMapper()

    private fun Map<String, String>.name() = get("official_name") ?: get("name")

    private fun relateStopsWithNames(responseObject: OverpassResponseBody): List<PhysicalStop> {
        val relationsMap = mutableMapOf<Long, OverpassElement>()
        val stopsMap = mutableMapOf<Long, OverpassElement>()
        for (element in responseObject.elements) {
            when (element.type) {
                "relation" -> {
                    relationsMap[element.id] = element
                }
                "node" -> {
                    stopsMap[element.id] = element
                }
                else -> {
                    log.warn("Unexpected element type ${element.type} in Overpass response")
                }
            }
        }
        for (relation in relationsMap.values) {
            val tags = relation.tags ?: continue
            val relationName = tags.name() ?: continue
            val members = relation.members ?: continue
            for (member in members) {
                val stop = stopsMap[member.ref] ?: continue
                val stopTags = stop.tags ?: emptyMap()
                val stopName = stopTags.name()
                if (stopName == null) {
                    stopsMap[member.ref] = stop.copy(tags = stopTags + ("name" to relationName))
                }
            }
        }
        return stopsMap.values.mapNotNull { element ->
            if (element.lon == null || element.lat == null) {
                log.debug("No coordinates for Overpass stop ${element.id}")
                return@mapNotNull null
            }
            val tags = element.tags ?: emptyMap()
            val name = tags.name()
            if (name == null) {
                log.debug("No name for Overpass stop ${element.id}")
                return@mapNotNull null
            }
            PhysicalStop(
                relationalId = null,
                externalId = element.id.toString(),
                name = name,
                position = geometryFactory.createPoint(Coordinate(element.lon, element.lat)),
                tags = tags,
            )
        }
    }

    override fun getPhysicalStops(): List<PhysicalStop> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("data", ALL_STOPS_QUERY)
        }
        val response = webClient.post()
            .uri("/interpreter")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(String::class.java)
            .blockOptional()
            .orElseThrow{ ServiceUnavailableException("Did not receive response from Overpass") }
        if (response.startsWith("<?xml") || response.startsWith("<!DOCTYPE")) {
            throw ServiceUnavailableException("Overpass returned error:\n$response")
        }
        val responseObject = objectMapper.readValue(response, OverpassResponseBody::class.java)
        return relateStopsWithNames(responseObject)
    }
}
