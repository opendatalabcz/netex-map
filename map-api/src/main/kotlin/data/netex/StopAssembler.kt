package cz.cvut.fit.gaierda1.data.netex

import cz.cvut.fit.gaierda1.data.orm.model.Stop
import cz.cvut.fit.gaierda1.data.orm.repository.StopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.TimetableParseResult
import org.rutebanken.netex.model.AccessFacilityEnumeration
import org.rutebanken.netex.model.AccessibilityInfoFacilityEnumeration
import org.rutebanken.netex.model.CateringFacilityEnumeration
import org.rutebanken.netex.model.MobilityFacilityEnumeration
import org.rutebanken.netex.model.ParkingFacilityEnumeration
import org.rutebanken.netex.model.SanitaryFacilityEnumeration
import org.rutebanken.netex.model.SiteFacilitySet
import org.springframework.stereotype.Component
import kotlin.collections.first
import kotlin.jvm.optionals.getOrNull

@Component
class StopAssembler(
    private val stopJpaRepository: StopJpaRepository,
) {
    fun assembleStops(
        registry: NetexFileRegistry,
        parseCache: TimetableParseResult,
    ): Map<String, Stop> {
        val lines = registry.lineRegistry.values
        if (lines.size != 1) error("Expected exactly one line, found $lines.size")
        val linePublicCode = lines.first().publicCode
        val stops = mutableMapOf<String, Stop>()
        for (stopPlace in registry.stopPlaceRegistry.values) {
            val fromCache = parseCache.findStop(linePublicCode, stopPlace.name.value)
            if (fromCache != null) {
                stops[stopPlace.id] = fromCache
                continue
            }
            val siteFacilitySet = stopPlace.facilities?.siteFacilitySetRefOrSiteFacilitySet as? SiteFacilitySet
            val stopId = stopJpaRepository.findIdByLinePublicCodeAndName(linePublicCode, stopPlace.name.value)
            val assembledStop = Stop(
                relationalId = stopId.getOrNull(),
                name = stopPlace.name.value,
                linePublicCode = linePublicCode,
                bistro = siteFacilitySet?.cateringFacilityList?.contains(
                    CateringFacilityEnumeration.BISTRO) ?: false,
                borderCrossing = stopPlace.isBorderCrossing ?: false,
                displaysForVisuallyImpaired = siteFacilitySet?.accessibilityInfoFacilityList?.contains(
                    AccessibilityInfoFacilityEnumeration.DISPLAYS_FOR_VISUALLY_IMPAIRED) ?: false,
                lowFloorAccess = siteFacilitySet?.accessFacilityList?.contains(
                    AccessFacilityEnumeration.LOW_FLOOR_ACCESS) ?: false,
                parkAndRidePark = siteFacilitySet?.parkingFacilityList?.contains(
                    ParkingFacilityEnumeration.PARK_AND_RIDE_PARK) ?: false,
                suitableForHeavilyDisabled = siteFacilitySet?.mobilityFacilityList?.contains(
                    MobilityFacilityEnumeration.SUITABLE_FOR_HEAVILIY_DISABLED) ?: false,
                toilet = siteFacilitySet?.sanitaryFacilityList?.run {
                        contains(SanitaryFacilityEnumeration.TOILET)
                            || contains(SanitaryFacilityEnumeration.WHEELCHAIR_ACCESS_TOILET)
                    } ?: false,
                wheelChairAccessToilet = siteFacilitySet?.sanitaryFacilityList?.contains(
                    SanitaryFacilityEnumeration.WHEELCHAIR_ACCESS_TOILET) ?: false,
                otherTransportModes =  stopPlace.otherTransportModes.joinToString().ifEmpty { null },
            )
            parseCache.addStop(assembledStop)
            stops[stopPlace.id] = assembledStop
        }
        return stops
    }
}
