import HttpRequestSender from '@/api/httpRequestSender'
import type { EncodedRoute } from '@/api/model/encodedRoute'

const ROUTE_URI = 'route'

const RouteApi = {
    getEncodedRoute(routeId: number): Promise<EncodedRoute | null | undefined> {
        return HttpRequestSender.get([ROUTE_URI, routeId + ''], null)
    },
}

export default RouteApi
