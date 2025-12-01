package cz.cvut.fit.gaierda1

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Unmarshaller
import org.rutebanken.netex.model.Common_VersionFrameStructure
import org.rutebanken.netex.model.PublicationDeliveryStructure
import java.io.File

fun main() {
    val xmlFile = File("src/data/city_lines/NX-PI-01_CZ_CISJR-JDF_LINE-1_20251022.xml")

    val context: JAXBContext = JAXBContext.newInstance(PublicationDeliveryStructure::class.java)
    val unmarshaller: Unmarshaller = context.createUnmarshaller()

    val publicationDelivery: PublicationDeliveryStructure = when (val result = unmarshaller.unmarshal(xmlFile)) {
        is JAXBElement<*> -> (result.value as? PublicationDeliveryStructure)?: error("Unexpected result type: ${result::class}")
        is PublicationDeliveryStructure -> result
        else -> error("Unexpected result type: ${result::class}")
    }

    val rootFrames: MutableList<JAXBElement<out Common_VersionFrameStructure>> = publicationDelivery.dataObjects.compositeFrameOrCommonFrame
    for (frame in rootFrames) {
        when (val frameValue = frame.value) {
            else -> println("Root frame of type: ${frameValue::class}")
        }
    }
}