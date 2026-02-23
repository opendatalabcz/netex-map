package cz.cvut.fit.gaierda1.domain.model

data class Page<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val pageRequest: PageRequest,
) {
    fun <S> map(transform: (T) -> S) = Page(
        content = content.map(transform),
        totalPages = totalPages,
        totalElements = totalElements,
        pageRequest = pageRequest
    )
}
