package cz.cvut.fit.gaierda1.data.netex.assembler

import cz.cvut.fit.gaierda1.domain.port.TimetableParserPort
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Unmarshaller
import org.rutebanken.netex.model.PublicationDeliveryStructure
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class NetexTimetableParser(
    private val netexFileIndexer: NetexFileIndexer,
    private val lineVersionAssembler: LineVersionAssembler,
    private val timetableStopAssembler: TimetableStopAssembler,
    private val operatingPeriodsAssembler: OperatingPeriodsAssembler,
    private val journeyAssembler: JourneyAssembler,
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

        val lineVersionsMap = lineVersionAssembler.assembleLineVersion(registry)
        val timetableStopsMap = timetableStopAssembler.assembleTimetableStops(registry)
        val operatingPeriodsMap = operatingPeriodsAssembler.assembleOperatingPeriods(registry)
        val journeysMap = journeyAssembler.assembleJourneys(registry, lineVersionsMap, timetableStopsMap, operatingPeriodsMap)

        val lineVersions = lineVersionsMap.values.toList()
        return TimetableParserPort.TimetableParseResult(
            lineVersions = lineVersions,
            timetableStops = timetableStopsMap.values.toList(),
            journeys = journeysMap.values.toList(),
        )
    }
}