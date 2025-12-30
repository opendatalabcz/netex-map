package cz.cvut.fit.gaierda1.domain

@JvmInline
value class JourneyId(val value: String)

data class Journey(
    val journeyId: JourneyId,
    val patternId: JourneyPatternLineLocalId,
    val schedule: List<TimetablePassingTime>,
    val operatingPeriod: OperatingPeriod,
) {
}
