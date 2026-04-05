package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.presentation.model.ModelConvertor
import cz.cvut.fit.gaierda1.domain.usecase.view.GetJourneysOperatingInFrameUseCase
import cz.cvut.fit.gaierda1.presentation.model.HttpJourneysOperatingInFrameResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/journey")
class JourneyController(
    private val modelConvertor: ModelConvertor,
    private val getJourneysOperatingInFrameUseCase: GetJourneysOperatingInFrameUseCase,
) {
    @GetMapping("/date-hour/{dateTime}")
    @ResponseBody
    fun getJourneysOperatingInFrame(
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
