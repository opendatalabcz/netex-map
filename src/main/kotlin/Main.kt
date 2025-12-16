package cz.cvut.fit.gaierda1

import cz.cvut.fit.gaierda1.convertor.netex.handleBusLineFile
import java.io.File

fun main() {
    val xmlFile = File("src/data/city_lines/NX-PI-01_CZ_CISJR-JDF_LINE-1_20251022.xml")
    handleBusLineFile(xmlFile)
}