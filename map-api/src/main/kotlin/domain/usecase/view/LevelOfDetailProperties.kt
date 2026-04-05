package cz.cvut.fit.gaierda1.domain.usecase.view

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "map.lod")
data class LevelOfDetailProperties(
    var minRouteLength: Map<Int, Double> = emptyMap(),
)
