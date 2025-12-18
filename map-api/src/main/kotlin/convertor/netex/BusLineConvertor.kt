package cz.cvut.fit.gaierda1.convertor.netex

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Unmarshaller
import org.rutebanken.netex.model.Common_VersionFrameStructure
import org.rutebanken.netex.model.CompositeFrame
import org.rutebanken.netex.model.PublicationDeliveryStructure
import org.rutebanken.netex.model.SiteFrame
import org.rutebanken.netex.model.StopPlace
import java.io.File

fun handleCompositeFrame(compositeFrame: CompositeFrame) {
    for (frame in compositeFrame.frames.commonFrame) {
        when (val frameValue = frame.value) {
            is SiteFrame -> {
                for (stopPlace in frameValue.stopPlaces.stopPlace_) {
                    when(val stopPlaceValue = stopPlace.value) {
                        is StopPlace -> stopPlaceValue.stopPlaceType
                    }
                }
            }
        }
    }
}

fun handleBusLineFile(xmlFile: File) {
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
            is CompositeFrame -> handleCompositeFrame(frameValue)
            else -> println("Root frame of type: ${frameValue::class}")
        }
    }
}
