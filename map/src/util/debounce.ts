const debounce = (callback: (...args: unknown[]) => void, wait: number) => {
    let timeoutId: number | undefined = undefined
    return (...args: unknown[]) => {
        window.clearTimeout(timeoutId)
        timeoutId = window.setTimeout(() => {
            callback(...args)
        }, wait)
    }
}

export { debounce }
