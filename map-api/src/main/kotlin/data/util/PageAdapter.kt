package cz.cvut.fit.gaierda1.data.util

import cz.cvut.fit.gaierda1.domain.model.Page as DomainPage
import cz.cvut.fit.gaierda1.domain.model.PageRequest as DomainPageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PageAdapter {
    fun toDomain(pageRequest: Pageable): DomainPageRequest = DomainPageRequest(pageRequest.pageNumber, pageRequest.pageSize)

    fun toData(pageRequest: DomainPageRequest): Pageable = PageRequest.of(pageRequest.page, pageRequest.size)

    fun <T : Any> toDomain(page: Page<T>): DomainPage<T> = DomainPage(
        content = page.content,
        totalPages = page.totalPages,
        totalElements = page.totalElements,
        pageRequest = toDomain(page.pageable),
    )

    fun <T : Any> toData(page: DomainPage<T>): Page<T> = PageImpl(
        page.content,
        toData(page.pageRequest),
        page.totalElements
    )
}
