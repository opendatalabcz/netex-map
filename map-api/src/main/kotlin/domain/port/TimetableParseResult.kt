package cz.cvut.fit.gaierda1.domain.port

import cz.cvut.fit.gaierda1.data.orm.model.Journey
import cz.cvut.fit.gaierda1.data.orm.model.JourneyPattern
import cz.cvut.fit.gaierda1.data.orm.model.LineVersion
import cz.cvut.fit.gaierda1.data.orm.model.OperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.Operator
import cz.cvut.fit.gaierda1.data.orm.model.Stop
import java.time.LocalDateTime
import java.time.OffsetDateTime

class TimetableParseResult {
    private data class LineVersionDomainKey(
        val publicCode: String,
        val isDetour: Boolean,
        val validFrom: OffsetDateTime,
        val validTo: OffsetDateTime,
    )
    private data class StopDomainKey(
        val linePublicCode: String,
        val name: String,
    )
    private data class OperatingPeriodDomainKey(
        val fromDate: LocalDateTime,
        val toDate: LocalDateTime,
        val validDays: List<Boolean>,
    )
    private data class JourneyDomainKey(
        val journeyNumber: String,
        val lineVersionKey: LineVersionDomainKey,
    )
    private data class JourneyPatternDomainKey(
        val patternNumber: Int,
        val lineVersionKey: LineVersionDomainKey,
    )
    private val _journeyPatterns = mutableMapOf<JourneyPatternDomainKey, JourneyPattern>()
    private val _journeys = mutableMapOf<JourneyDomainKey, Journey>()
    private val _lineVersions = mutableMapOf<LineVersionDomainKey, LineVersion>()
    private val _operatingPeriods = mutableMapOf<OperatingPeriodDomainKey, OperatingPeriod>()
    private val _operators = mutableMapOf<String, Operator>()
    private val _stops = mutableMapOf<StopDomainKey, Stop>()

    val journeyPatterns: List<JourneyPattern>
        get() = _journeyPatterns.values.toList()
    val journeys: List<Journey>
        get() = _journeys.values.toList()
    val lineVersions: List<LineVersion>
        get() = _lineVersions.values.toList()
    val operatingPeriods: List<OperatingPeriod>
        get() = _operatingPeriods.values.toList()
    val operators: List<Operator>
        get() = _operators.values.toList()
    val stops: List<Stop>
        get() = _stops.values.toList()

    fun addLineVersion(lineVersion: LineVersion) {
        val key = LineVersionDomainKey(
            publicCode = lineVersion.publicCode,
            isDetour = lineVersion.isDetour,
            validFrom = lineVersion.validFrom,
            validTo = lineVersion.validTo,
        )
        _lineVersions[key] = lineVersion
    }

    fun findLineVersion(
        publicCode: String,
        isDetour: Boolean,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
    ): LineVersion? {
        val key = LineVersionDomainKey(
            publicCode = publicCode,
            isDetour = isDetour,
            validFrom = validFrom,
            validTo = validTo,
        )
        return _lineVersions[key]
    }

    fun addOperatingPeriod(operatingPeriod: OperatingPeriod) {
        val key = OperatingPeriodDomainKey(
            fromDate = operatingPeriod.fromDate,
            toDate = operatingPeriod.toDate,
            validDays = operatingPeriod.validDays,
        )
        _operatingPeriods[key] = operatingPeriod
    }

    fun findOperatingPeriod(
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        validDays: List<Boolean>,
    ): OperatingPeriod? {
        val key = OperatingPeriodDomainKey(
            fromDate = fromDate,
            toDate = toDate,
            validDays = validDays,
        )
        return _operatingPeriods[key]
    }

    fun addOperator(operator: Operator) {
        val key = operator.publicCode
        _operators[key] = operator
    }

    fun findOperator(publicCode: String): Operator? {
        return _operators[publicCode]
    }

    fun addStop(stop: Stop) {
        val key = StopDomainKey(
            linePublicCode = stop.linePublicCode,
            name = stop.name,
        )
        _stops[key] = stop
    }

    fun findStop(linePublicCode: String, name: String): Stop? {
        val key = StopDomainKey(
            linePublicCode = linePublicCode,
            name = name,
        )
        return _stops[key]
    }

    fun addJourney(journey: Journey) {
        val key = JourneyDomainKey(
            journeyNumber = journey.journeyNumber,
            lineVersionKey = LineVersionDomainKey(
                publicCode = journey.journeyPattern.lineVersion.publicCode,
                isDetour = journey.journeyPattern.lineVersion.isDetour,
                validFrom = journey.journeyPattern.lineVersion.validFrom,
                validTo = journey.journeyPattern.lineVersion.validTo,
            )
        )
        _journeys[key] = journey
    }

    fun findJourney(
        journeyNumber: String,
        publicCode: String,
        isDetour: Boolean,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
    ): Journey? {
        val lineVersionKey = LineVersionDomainKey(
            publicCode = publicCode,
            isDetour = isDetour,
            validFrom = validFrom,
            validTo = validTo,
        )
        val key = JourneyDomainKey(
            journeyNumber = journeyNumber,
            lineVersionKey = lineVersionKey,
        )
        return _journeys[key]
    }

    fun addJourneyPattern(journeyPattern: JourneyPattern) {
        val key = JourneyPatternDomainKey(
            patternNumber = journeyPattern.patternId.patternNumber,
            lineVersionKey = LineVersionDomainKey(
                publicCode = journeyPattern.lineVersion.publicCode,
                isDetour = journeyPattern.lineVersion.isDetour,
                validFrom = journeyPattern.lineVersion.validFrom,
                validTo = journeyPattern.lineVersion.validTo,
            )
        )
        _journeyPatterns[key] = journeyPattern
    }

    fun findJourneyPattern(
        patternNumber: Int,
        publicCode: String,
        isDetour: Boolean,
        validFrom: OffsetDateTime,
        validTo: OffsetDateTime,
    ): JourneyPattern? {
        val lineVersionKey = LineVersionDomainKey(
            publicCode = publicCode,
            isDetour = isDetour,
            validFrom = validFrom,
            validTo = validTo,
        )
        val key = JourneyPatternDomainKey(
            patternNumber = patternNumber,
            lineVersionKey = lineVersionKey,
        )
        return _journeyPatterns[key]
    }
}