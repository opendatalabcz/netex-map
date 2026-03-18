package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.presentation.model.HttpJourney
import cz.cvut.fit.gaierda1.presentation.model.ModelConvertor
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.domain.usecase.GetJourneysOperatingInFrameUseCase
import cz.cvut.fit.gaierda1.presentation.model.HttpJourneysOperatingInDayResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/journey")
class JourneyView(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val modelConvertor: ModelConvertor,
    private val getJourneysOperatingInFrameUseCase: GetJourneysOperatingInFrameUseCase,
) {
    @GetMapping
    @ResponseBody
    fun getJourneys(
        @RequestParam(required = false, defaultValue = "false") latitudeFirst: Boolean,
        pageable: Pageable?,
    ): Page<HttpJourney> {
        return journeyJpaRepository
            .findAllFetchRoutes(pageable ?: PageRequest.of(0, 100))
            .map { modelConvertor.toHttp(it, latitudeFirst) }
    }

    @GetMapping("/day/{day}")
    @ResponseBody
    fun getJourneysOperatingInDay(
        @RequestParam lonMin: Double,
        @RequestParam latMin: Double,
        @RequestParam lonMax: Double,
        @RequestParam latMax: Double,
        @PathVariable day: LocalDate,
        @RequestParam(required = false, defaultValue = "UTC") timezone: String,
    ): HttpJourneysOperatingInDayResult {
        val zone = try {
            ZoneId.of(timezone)
        } catch (e: DateTimeException) {
            ZoneId.of("UTC")
        }
        return modelConvertor.toHttp(
            getJourneysOperatingInFrameUseCase.getJourneysOperatingInFrame(
                lonMin, latMin, lonMax, latMax, day, zone
            )
        )
    }
}
