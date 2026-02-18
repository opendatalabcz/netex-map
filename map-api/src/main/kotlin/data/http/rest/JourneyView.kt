package cz.cvut.fit.gaierda1.data.http.rest

import cz.cvut.fit.gaierda1.data.http.adapter.DomainModelAdapter
import cz.cvut.fit.gaierda1.data.http.model.HttpJourney
import cz.cvut.fit.gaierda1.domain.port.JourneyViewPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/journey")
class JourneyView(
    private val journeyViewPort: JourneyViewPort,
    private val domainModelAdapter: DomainModelAdapter,
) {

    @GetMapping
    fun getJourneys(pageable: Pageable): Page<HttpJourney> {
        return journeyViewPort.getJourneys(pageable).map(domainModelAdapter::toHttp)
    }
}
