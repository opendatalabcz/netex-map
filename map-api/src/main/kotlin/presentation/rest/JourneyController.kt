package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.presentation.model.HttpJourney
import cz.cvut.fit.gaierda1.presentation.model.ModelConvertor
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneysOperatingInFrameUseCase
import cz.cvut.fit.gaierda1.presentation.model.HttpJourneysOperatingInFrameResult
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.Optional

@RestController
@RequestMapping("/journey")
class JourneyController(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val modelConvertor: ModelConvertor,
    private val getJourneysOperatingInFrameUseCase: GetJourneysOperatingInFrameUseCase,
) {
    @GetMapping("/{id}")
    @ResponseBody
    fun getJourney(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "false") includeRoute: Boolean,
        @RequestParam(required = false, defaultValue = "false") latitudeFirst: Boolean,
    ): HttpJourney {
        val searchResult: Optional<Journey> =
            if (includeRoute) journeyJpaRepository.findByIdFetchRoute(id)
            else journeyJpaRepository.findById(id)
        return searchResult
            .map { modelConvertor.toHttp(it, includeRoute, latitudeFirst) }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
    }

    @GetMapping("/date-hour/{dateTime}")
    @ResponseBody
    fun getJourneysOperatingInDay(
        @RequestParam lonMin: Double,
        @RequestParam latMin: Double,
        @RequestParam lonMax: Double,
        @RequestParam latMax: Double,
        @RequestParam zoom: Int,
        @PathVariable dateTime: OffsetDateTime,
    ): HttpJourneysOperatingInFrameResult {
        return modelConvertor.toHttp(
            getJourneysOperatingInFrameUseCase.getJourneysOperatingInFrame(
                lonMin, latMin, lonMax, latMax, zoom, dateTime
            )
        )
    }
}
