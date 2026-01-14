package cz.cvut.fit.gaierda1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)

    val xmlFile = File("src/data/city_lines/NX-PI-01_CZ_CISJR-JDF_LINE-1_20251022.xml")
}
