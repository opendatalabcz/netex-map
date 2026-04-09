package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.domain.usecase.view.ConstructWallTimetableUseCase
import cz.cvut.fit.gaierda1.domain.usecase.view.SearchLineVersionsUseCase
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/line-version")
class LineVersionController(
    private val constructWallTimetableUseCase: ConstructWallTimetableUseCase,
    private val searchLineVersionsUseCase: SearchLineVersionsUseCase,
) {
    @GetMapping
    fun searchLineVersions(
        @RequestParam query: String,
        pageable: Pageable?,
    ): Page<SearchLineVersionsUseCase.SearchLineVersion> {
        return searchLineVersionsUseCase.search(query, pageable)
    }

    @GetMapping("/{id}/wall")
    fun constructWallTimetable(
        @PathVariable id: Long,
    ): ConstructWallTimetableUseCase.WallTimetable {
        return constructWallTimetableUseCase
            .constructWallTimetable(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
