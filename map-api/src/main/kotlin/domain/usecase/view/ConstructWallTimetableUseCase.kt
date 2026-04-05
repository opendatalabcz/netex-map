package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.model.JourneyDirectionType
import cz.cvut.fit.gaierda1.data.orm.model.LineType
import cz.cvut.fit.gaierda1.data.orm.repository.dto.OperatorDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.ActivePeriodWallDto
import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.StopWallDto
import java.time.LocalDate
import java.time.LocalTime

interface ConstructWallTimetableUseCase {
    data class WallScheduledStop(
        val arrival: LocalTime?,
        val departure: LocalTime?,
    )
    data class WallOperatingDays(
        val monday: Boolean,
        val tuesday: Boolean,
        val wednesday: Boolean,
        val thursday: Boolean,
        val friday: Boolean,
        val saturday: Boolean,
        val sunday: Boolean,
    )
    enum class WallOperationExceptionType {
        ALSO_OPERATES,
        DOES_NOT_OPERATE,
    }
    data class WallTariffStop(
        val tariffZone: String?,
        val stopId: Long,
    )
    data class WallLineVersion(
        val relationalId: Long,
        val publicCode: String,
        val name: String,
        val shortName: String,
        val transportMode: String,
        val lineType: LineType,
        val isDetour: Boolean,
        val operator: OperatorDto,
        val activePeriods: List<ActivePeriodWallDto>,
        val tariffStops: List<WallTariffStop>,
        val stops: List<StopWallDto>,
    )
    data class WallJourney(
        val relationalId: Long,
        val schedule: List<WallScheduledStop>,
        val requiresOrdering: Boolean,
        val baggageStorage: Boolean,
        val cyclesAllowed: Boolean,
        val lowFloorAccess: Boolean,
        val reservationCompulsory: Boolean,
        val reservationPossible: Boolean,
        val snacksOnBoard: Boolean,
        val unaccompaniedMinorAssistance: Boolean,
    )
    data class WallJourneyPatternStop(
        val tariffOrder: Int,
        val distanceToNextStop: Double,
        val forBoarding: Boolean,
        val forAlighting: Boolean,
        val requiresOrdering: Boolean,
        val stopOnRequest: Boolean,
    )
    data class WallJourneyPattern(
        val patternNumber: Int,
        val direction: JourneyDirectionType,
        val stops: List<WallJourneyPatternStop>,
        val transportBans: List<List<Int>>?,
        val routeId: Long,
    )
    data class WallOperatingPeriod(
        val operatingDays: WallOperatingDays,
        val operationExceptions: Map<WallOperationExceptionType, List<LocalDate>>,
        val journeys: List<WallJourney>,
    )
    data class WallTimetable(
        val lineVersion: WallLineVersion,
        val operatingPeriods: List<WallOperatingPeriod>,
        val journeyPatterns: List<WallJourneyPattern>,
    )

    fun constructWallTimetable(lineVersionId: Long): WallTimetable?
}
