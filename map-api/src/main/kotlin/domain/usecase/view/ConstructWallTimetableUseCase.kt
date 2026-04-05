package cz.cvut.fit.gaierda1.domain.usecase.view

import cz.cvut.fit.gaierda1.data.orm.repository.dto.wall.ActivePeriodWallDto
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
    data class WallLineVersion(
        val relationalId: Long,
        val publicCode: String,
        val name: String,
        val shortName: String,
        val transportMode: String,
        val isDetour: Boolean,
        val activePeriods: List<ActivePeriodWallDto>,
    )
    data class WallOperatingPeriod(
        val operatingDays: WallOperatingDays,
        val operationExceptions: Map<WallOperationExceptionType, List<LocalDate>>,
        val journeys: Map<Long, List<WallScheduledStop>>,
    )
    data class WallTimetable(
        val lineVersion: WallLineVersion,
        val operatingPeriods: List<WallOperatingPeriod>,
    )

    fun constructWallTimetable(lineVersionId: Long): WallTimetable?
}
