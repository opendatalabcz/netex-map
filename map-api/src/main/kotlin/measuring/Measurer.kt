package cz.cvut.fit.gaierda1.measuring

import kotlin.time.Duration
import kotlin.time.measureTime

object Measurer {
    var xmlParse: Duration = Duration.ZERO
    var dbFind: Duration = Duration.ZERO
    var dbSave: Duration = Duration.ZERO
    var appInitiatedFinds = 0

    var searchedJourneys = 0
    var searchedScheduledStops = 0
    var searchedPhysicalStops = 0
    var searchedRouteStops = 0
    var searchedOperatingPeriods = 0
    var searchedLineVersions = 0
    var searchedRoutes = 0

    var savedJourneys = 0
    var savedScheduledStops = 0
    var savedPhysicalStops = 0
    var savedRouteStops = 0
    var savedOperatingPeriods = 0
    var savedLineVersions = 0
    var savedRoutes = 0

    fun <T> addToXmlParse(workLoad: () -> T): T {
        val res: T
        xmlParse += measureTime {
            res = workLoad()
        }
        return res
    }

    fun <T> addToDbFind(workLoad: () -> T): T {
        ++appInitiatedFinds
        val res: T
        dbFind += measureTime {
            res = workLoad()
        }
        return res
    }

    fun <T> addToDbSave(workLoad: () -> T): T {
        val res: T
        dbSave += measureTime {
            res = workLoad()
        }
        return res
    }
}
