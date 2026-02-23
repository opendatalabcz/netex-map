package cz.cvut.fit.gaierda1.data.netex.domain

import cz.cvut.fit.gaierda1.data.netex.NetexFileIndexer
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
    private val lineVersionDomainAssembler: LineVersionDomainAssembler,
    private val operatingPeriodsDomainAssembler: OperatingPeriodsDomainAssembler,
    private val journeyDomainAssembler: JourneyDomainAssembler,
): TimetableParserPort {
    override fun parseTimetable(contentStream: InputStream): TimetableParserPort.TimetableParseResult {
        val jaxbContext: JAXBContext = JAXBContext.newInstance(PublicationDeliveryStructure::class.java)
        val unmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

        val publicationDelivery: PublicationDeliveryStructure = when (val result = unmarshaller.unmarshal(contentStream)) {
            is JAXBElement<*> -> (result.value as? PublicationDeliveryStructure)?: error("Unexpected result type: ${result::class}")
            is PublicationDeliveryStructure -> result
            else -> error("Unexpected result type: ${result::class}")
        }
        val registry = netexFileIndexer.createRegistry(publicationDelivery)

        val lineVersionsMap = lineVersionDomainAssembler.assembleLineVersions(registry)
        val operatingPeriodsMap = operatingPeriodsDomainAssembler.assembleOperatingPeriods(registry)
        val journeysMap = journeyDomainAssembler.assembleJourneys(registry, lineVersionsMap, operatingPeriodsMap)

        return TimetableParserPort.TimetableParseResult(
            lineVersions = lineVersionsMap.values.toList(),
            operatingPeriods = operatingPeriodsMap.values.toList(),
            journeys = journeysMap.values.toList(),
        )
    }
}