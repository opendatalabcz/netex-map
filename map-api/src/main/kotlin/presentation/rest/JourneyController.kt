package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneyDetailsUseCase
import cz.cvut.fit.gaierda1.presentation.model.ModelConvertor
import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneysOperatingInFrameUseCase
import cz.cvut.fit.gaierda1.presentation.model.HttpJourneysOperatingInFrameResult
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@RestController
@RequestMapping("/journey")
class JourneyController(
    private val modelConvertor: ModelConvertor,
    private val getJourneysOperatingInFrameUseCase: GetJourneysOperatingInFrameUseCase,
    private val getJourneyDetailsUseCase: GetJourneyDetailsUseCase,
) {

    @GetMapping("/{id}/details")
    fun getJourneyDetails(
        @PathVariable id: Long,
    ): GetJourneyDetailsUseCase.JourneyDetails {
        return getJourneyDetailsUseCase
            .getJourneyDetails(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/date-hour/{dateTime}")
    fun getJourneysOperatingInFrame(
        @RequestParam lonMin: Double,
        @RequestParam latMin: Double,
        @RequestParam lonMax: Double,
        @RequestParam latMax: Double,
        @RequestParam zoom: Int,
        @PathVariable dateTime: OffsetDateTime,
        @RequestParam(required = false) nj: Set<Long> = emptySet(),
        @RequestParam(required = false) njpd: Set<Long> = emptySet(),
        @RequestParam(required = false) nr: Set<Long> = emptySet(),
    ): HttpJourneysOperatingInFrameResult {
        return modelConvertor.toHttp(
            getJourneysOperatingInFrameUseCase.getJourneysOperatingInFrame(
                lonMin, latMin, lonMax, latMax, zoom, dateTime,
                nj, njpd, nr,
            )
        )
    }
}
