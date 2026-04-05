package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
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
    private val operatorAssembler: OperatorAssembler,
    private val journeyAssembler: JourneyAssembler,
    private val stopAssembler: StopAssembler,
    private val journeyPatternAssembler: JourneyPatternAssembler,
): TimetableParserPort {
    private val jaxbContext: JAXBContext by lazy {
        JAXBContext.newInstance(PublicationDeliveryStructure::class.java)
    }

    override fun parseTimetable(
        contentStream: InputStream,
        cumulativeParseResult: TimetableParseResult?,
    ): TimetableParseResult {
        val unmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

        val publicationDelivery: PublicationDeliveryStructure = when (
            val result = unmarshaller.unmarshal(contentStream)
        ) {
            is JAXBElement<*> -> (result.value as? PublicationDeliveryStructure)?: error("Unexpected result type: ${result::class}")
            is PublicationDeliveryStructure -> result
            else -> error("Unexpected result type: ${result::class}")
        }
        val registry = netexFileIndexer.createRegistry(publicationDelivery)

        val result = cumulativeParseResult ?: TimetableParseResult()
        val operatorsMap = operatorAssembler.assembleOperators(registry, result)
        val stopsMap = stopAssembler.assembleStops(registry, result)
        val lineVersionsMap = lineVersionAssembler.assembleLineVersions(registry, operatorsMap, stopsMap, result)
        if (lineVersionsMap.isEmpty()) {
            return result
        }
        val operatingPeriodsMap = operatingPeriodsAssembler.assembleOperatingPeriods(registry, result)
        val journeyPatternsMap = journeyPatternAssembler.assembleJourneyPatterns(registry, lineVersionsMap, result)
        val journeysMap = journeyAssembler.assembleJourneys(registry, journeyPatternsMap, operatingPeriodsMap, result)

        return result
    }
}
