package cz.cvut.fit.gaierda1.data.orm.adapter

import cz.cvut.fit.gaierda1.data.orm.mapper.JourneyMapper
import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.model.DateRange
import cz.cvut.fit.gaierda1.domain.model.Journey
import cz.cvut.fit.gaierda1.domain.model.JourneyId
import cz.cvut.fit.gaierda1.domain.model.LineId
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import org.springframework.stereotype.Component

@Component
class JourneyRepositoryAdapter(
    private val journeyJpaRepository: JourneyJpaRepository,
    private val scheduledStopJpaRepository: ScheduledStopJpaRepository,
    private val journeyMapper: JourneyMapper,
): JourneyRepository {
    override fun save(journey: Journey) {
        val dbJourney = journeyMapper.toDb(journey)
        journeyJpaRepository.save(dbJourney)
        for (scheduledStop in dbJourney.schedule) {
            scheduledStop.id.journeyId = dbJourney.relationalId
            scheduledStopJpaRepository.save(scheduledStop)
        }
    }

    override fun findById(lineId: LineId, validRange: DateRange, journeyId: JourneyId): Journey? {
        return journeyJpaRepository.findByExternalIdAndLineIdAndValidRange(
            externalId = journeyId.value,
            lineExternalId = lineId.value,
            validFrom = validRange.from,
            validTo = validRange.to,
            timezone = validRange.timezone,
        ).map(journeyMapper::toDomain)
            .orElse(null)
    }
}
