package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.domain.usecase.view.ConstructWallTimetableUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/line-version")
class LineVersionController(
    private val constructWallTimetableUseCase: ConstructWallTimetableUseCase,
) {
    @RequestMapping("/{id}/wall")
    fun constructWallTimetable(
        @PathVariable id: Long,
    ): ConstructWallTimetableUseCase.WallTimetable {
        return constructWallTimetableUseCase
            .constructWallTimetable(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
