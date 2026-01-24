package cz.cvut.fit.gaierda1.data.netex.data

import cz.cvut.fit.gaierda1.data.netex.NetexFileIndexer
import cz.cvut.fit.gaierda1.domain.port.TimetableParserDataPort
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Unmarshaller
import org.rutebanken.netex.model.PublicationDeliveryStructure
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class TimetableDataParser(
    private val netexFileIndexer: NetexFileIndexer,
    private val lineVersionDataAssembler: LineVersionDataAssembler,
    private val operatingPeriodsDataAssembler: OperatingPeriodsDataAssembler,
    private val journeyDataAssembler: JourneyDataAssembler,
): TimetableParserDataPort {
    override fun parseTimetable(contentStream: InputStream): TimetableParserDataPort.TimetableParseResult {
        val jaxbContext: JAXBContext = JAXBContext.newInstance(PublicationDeliveryStructure::class.java)
        val unmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

        val publicationDelivery: PublicationDeliveryStructure = when (val result = unmarshaller.unmarshal(contentStream)) {
            is JAXBElement<*> -> (result.value as? PublicationDeliveryStructure)?: error("Unexpected result type: ${result::class}")
            is PublicationDeliveryStructure -> result
            else -> error("Unexpected result type: ${result::class}")
        }
        val registry = netexFileIndexer.createRegistry(publicationDelivery)

        val lineVersionsMap = lineVersionDataAssembler.assembleLineVersions(registry)
        val operatingPeriodsMap = operatingPeriodsDataAssembler.assembleOperatingPeriods(registry)
        val journeysMap = journeyDataAssembler.assembleJourneys(registry, lineVersionsMap, operatingPeriodsMap)

        return TimetableParserDataPort.TimetableParseResult(
            lineVersions = lineVersionsMap.values.toList(),
            journeys = journeysMap.values.toList(),
        )
    }
}