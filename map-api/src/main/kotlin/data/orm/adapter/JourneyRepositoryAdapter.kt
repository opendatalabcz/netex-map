package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.JourneyMapper
import cz.cvut.fit.gaierda1.data.orm.model.DbJourney
import cz.cvut.fit.gaierda1.data.orm.model.DbOperatingPeriod
import cz.cvut.fit.gaierda1.data.orm.model.DbScheduledStop
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JourneyRepositoryAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
    private val journeyMapper: JourneyMapper,
): JourneyRepository {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun save(journey: Journey) {
        val mapped = journeyMapper.toDb(journey)
        val optionalSaved = journeyJpaRepository.findByExternalIdAndLineIdAndValidRange(
            externalId = journey.journeyId.value,
            lineExternalId = journey.lineVersion.line.lineId.value,
            validFrom = journey.lineVersion.validIn.from,
            validTo = journey.lineVersion.validIn.to,
            timezone = journey.lineVersion.validIn.timezone,
        )
        optionalSaved.ifPresent { saved ->
            mapped.relationalId = saved.relationalId
            checkAndLogDifference("journey pattern id", saved.journeyPatternId, mapped.journeyPatternId, journey)
        }

        saveOperatingPeriods(mapped, optionalSaved.map { it.operatingPeriods }.orElse(null), journey)
        journeyJpaRepository.save(mapped)
        for (scheduledStop in mapped.schedule) {
            scheduledStop.id.journeyId = mapped.relationalId
        }
        saveScheduledStops(mapped.schedule, optionalSaved.map { it.schedule }.orElse(null), journey)
    }
    
    private fun saveScheduledStops(
        newScheduledStops: List<DbScheduledStop>,
        oldScheduledStops: List<DbScheduledStop>?,
        journey: Journey,
    ) {
        if (oldScheduledStops == null) {
            scheduledStopJpaRepository.saveAll(newScheduledStops)
            return
        }

        val maxSize = newScheduledStops.size.coerceAtLeast(oldScheduledStops.size)
        val newScheduledStopsIterator = newScheduledStops.iterator()
        val oldScheduledStopsIterator = oldScheduledStops.iterator()
        for (i in 0 until maxSize) {
            if (i < newScheduledStops.size && i < oldScheduledStops.size) {
                val old = oldScheduledStopsIterator.next()
                val new = newScheduledStopsIterator.next()
                checkAndLogDifference(i, old, new, journey)
                scheduledStopJpaRepository.save(new)
            } else if (i < newScheduledStops.size) {
                checkAndLogDifference("scheduled stop [${i + 1}]", null, "existing", journey)
                scheduledStopJpaRepository.save(newScheduledStopsIterator.next())
            } else {
                checkAndLogDifference("scheduled stop [${i + 1}]", "existing", null, journey)
            }
        }
    }

    private fun saveOperatingPeriods(
        newJourney: DbJourney,
        oldOperatingPeriods: List<DbOperatingPeriod>?,
        journey: Journey,
    ) {
        for (operatingPeriod in newJourney.operatingPeriods) {
            val optionalSaved = operatingPeriodJpaRepository.findByLineVersionIdAndValidDays(
                lineExternalId = newJourney.lineVersion.line.externalId,
                validFrom = newJourney.lineVersion.validFrom,
                validTo = newJourney.lineVersion.validTo,
                timezone = newJourney.lineVersion.timezone,
                validDays = operatingPeriod.validDays,
            )
            optionalSaved.ifPresent { saved ->
                operatingPeriod.relationalId = saved.relationalId
            }
        }

        if (oldOperatingPeriods != null) {
            val idUnionSet = (newJourney.operatingPeriods.map { it.relationalId } + oldOperatingPeriods.map { it.relationalId }).toSet()
            for (id in idUnionSet) {
                val new = newJourney.operatingPeriods.find { it.relationalId == id }
                val old = oldOperatingPeriods.find { it.relationalId == id }
                if (new != null && old != null) continue
                if (new != null) checkAndLogDifference("one of operating periods", null, new.validDays.map { if (it) '1' else '0' }, journey)
                else checkAndLogDifference("one of operating periods", old!!.validDays.map { if (it) '1' else '0' }, null, journey)
            }
        }

        operatingPeriodJpaRepository.saveAll(newJourney.operatingPeriods)
    }

    override fun findById(lineId: LineId, validRange: DateRange, journeyId: JourneyId): Journey? {
        return journeyJpaRepository
            .findByExternalIdAndLineIdAndValidRange(
                externalId = journeyId.value,
                lineExternalId = lineId.value,
                validFrom = validRange.from,
                validTo = validRange.to,
                timezone = validRange.timezone,
            ).map(journeyMapper::toDomain)
            .orElse(null)
    }

    private fun checkAndLogDifference(
        index: Int,
        old: DbScheduledStop,
        new: DbScheduledStop,
        journey: Journey,
    ) {
        checkAndLogDifference("scheduled stop [${index + 1}]: stop id", old.timetableStop.externalId, new.timetableStop.externalId, journey)
        checkAndLogDifference("scheduled stop [${index + 1}]: stop name", old.timetableStop.name, new.timetableStop.name, journey)
        checkAndLogDifference("scheduled stop [${index + 1}]: arrival", old.arrival, new.arrival, journey)
        checkAndLogDifference("scheduled stop [${index + 1}]: departure", old.departure, new.departure, journey)
    }

    private fun checkAndLogDifference(fieldName: String, old: Any?, new: Any?, context: Journey) {
        if (old != new) {
            log.warn(
                "Journey {} for line version {} {}-{}({}): {} changed from {} to {}",
                context.journeyId.value,
                context.lineVersion.line.lineId.value,
                context.lineVersion.validIn.from,
                context.lineVersion.validIn.to,
                context.lineVersion.validIn.timezone,
                fieldName,
                old,
                new
            )
        }
    }
}
