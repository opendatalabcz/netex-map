package cz.cvut.fit.gaierda1.measuring

import kotlin.time.Duration
import kotlin.time.measureTime

object Measurer {
    var xmlParse: Duration = Duration.ZERO
    var dbFind: Duration = Duration.ZERO
    var dbSave: Duration = Duration.ZERO

    fun <T> addToXmlParse(workLoad: () -> T): T {
        val res: T
        xmlParse += measureTime {
            res = workLoad()
        }
        return res
    }

    fun <T> addToDbFind(workLoad: () -> T): T {
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
