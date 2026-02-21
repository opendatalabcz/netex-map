type Page<T> = {
    content: T[],
    page: {
        size: number,
        number: number,
        totalElements: number,
        totalPages: number,
    },
}

type PageRequest = {
    page: number,
    size: number,
}

export type { Page, PageRequest }
