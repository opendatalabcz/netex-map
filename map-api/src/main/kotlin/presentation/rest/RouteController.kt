package cz.cvut.fit.gaierda1.presentation.rest

import cz.cvut.fit.gaierda1.domain.usecase.view.GetEncodedRoutesUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/route")
class RouteController(
    private val getEncodedRoutesUseCase: GetEncodedRoutesUseCase,
) {
    @GetMapping("/{id}")
    fun getRoute(
        @PathVariable id: Long,
    ): GetEncodedRoutesUseCase.EncodedRoute {
        return getEncodedRoutesUseCase
            .getEncodedRoutes(listOf(id))
            .firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}
