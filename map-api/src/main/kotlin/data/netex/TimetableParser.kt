package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Unmarshaller
import org.rutebanken.netex.model.PublicationDeliveryStructure
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class TimetableParser(
    private val netexFileIndexer: NetexFileIndexer,
    private val lineVersionAssembler: LineVersionAssembler,
    private val operatingPeriodsAssembler: OperatingPeriodsAssembler,
    private val journeyAssembler: JourneyAssembler,
): TimetableParserPort {
    private val jaxbContext: JAXBContext by lazy {
        JAXBContext.newInstance(PublicationDeliveryStructure::class.java)
    }

    override fun parseTimetable(contentStream: InputStream): TimetableParserPort.TimetableParseResult {
        val unmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

        val publicationDelivery: PublicationDeliveryStructure = when (
            val result = unmarshaller.unmarshal(contentStream)
        ) {
            is JAXBElement<*> -> (result.value as? PublicationDeliveryStructure)?: error("Unexpected result type: ${result::class}")
            is PublicationDeliveryStructure -> result
            else -> error("Unexpected result type: ${result::class}")
        }
        val registry = netexFileIndexer.createRegistry(publicationDelivery)

        val lineVersionsMap = lineVersionAssembler.assembleLineVersions(registry)
        val operatingPeriodsMap = operatingPeriodsAssembler.assembleOperatingPeriods(registry)
        val journeysMap = journeyAssembler.assembleJourneys(registry, lineVersionsMap, operatingPeriodsMap)

        return TimetableParserPort.TimetableParseResult(
            lineVersions = lineVersionsMap.values.toList(),
            operatingPeriods = operatingPeriodsMap.values.toList(),
            journeys = journeysMap.values.toList(),
        )
    }
}