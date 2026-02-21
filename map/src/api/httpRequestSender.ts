type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

const VITE_API_URL = import.meta.env.CLIENT_API_URL

const ResponseCodes = {
    OK: 200,
    BAD_REQUEST: 400,
    NOT_FOUND: 404,
    INTERNAL_SERVER_ERROR: 500,
}

function encodeToURI(key: string, data: unknown): string {
    if (Array.isArray(data)) {
        return Object.values(data)
            .map((d) => `${key}=${encodeURIComponent(d + '')}`)
            .join('&')
    }
    if (typeof data === 'string' || typeof data === 'number' || typeof data === 'boolean') {
        return `${key}=${encodeURIComponent(data)}`
    }
    return ''
}

async function request(
    path: string[],
    method: Method,
    query: object | null | undefined,
    data: object | null | undefined,
): Promise<Response | null | undefined> {
    const uri = path.map((s) => encodeURIComponent(s)).join('/')
    const urlQuery = query
        ? '?' +
          Object.entries(query)
              .map((entry) => encodeToURI(entry[0], entry[1]))
              .join('&')
        : ''

    const headers: Record<string, string> = {}
    let body: string | null = null

    if (data) {
        headers['Content-Type'] = 'application/json'
        body = JSON.stringify(data)
    }

    try {
        const response = await fetch(VITE_API_URL + uri + urlQuery, {
            method: method,
            headers: headers,
            body: body,
        })
        if (!response.ok) {
            // TODO
            console.warn('Response was not OK')
            return null
        }
        return response
    } catch (error) {
        if (error instanceof TypeError) {
            // TODO
            console.warn('Connection error')
        } else if (error instanceof DOMException) {
            if (error.name === 'AbortError') {
                // TODO
                console.warn('Request aborted')
            } else {
                // TODO
                console.warn('Unknown request error')
            }
        }
        return undefined
    }
}

const HttpRequestSender = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    async get(path: string[], query: object | null | undefined): Promise<any | null | undefined> {
        const response = await request(path, 'GET', query, null)
        if (!response) return response
        if (response.status !== ResponseCodes.OK) return null
        return response.json()
    },

    post(path: string[], data: object | null | undefined) {
        return request(path, 'POST', null, data)
    },

    put(path: string[], data: object) {
        return request(path, 'PUT', null, data)
    },

    patch(path: string[], data: object) {
        return request(path, 'PATCH', null, data)
    },

    delete(path: string[]) {
        return request(path, 'DELETE', null, null)
    },
}

export default HttpRequestSender
