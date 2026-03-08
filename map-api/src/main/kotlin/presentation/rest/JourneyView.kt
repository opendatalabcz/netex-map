package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.presentation.model.HttpJourney
import cz.cvut.fit.gaierda1.presentation.model.ModelConvertor
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/journey")
class JourneyView(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val modelConvertor: ModelConvertor,
) {
    @GetMapping
    fun getJourneys(
        @RequestParam(required = false, defaultValue = "false") latitudeFirst: Boolean,
        @ModelAttribute pageable: Pageable,
    ): Page<HttpJourney> {
        return journeyJpaRepository.findAll(pageable).map { modelConvertor.toHttp(it, latitudeFirst) }
    }
}
