package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.usecase.ImportDataTimetableUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ImportDomainTimetablesUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfiguration {
    @Bean
    fun importDomainTimetablesUseCase(
        lineVersionRepository: LineVersionRepository,
        journeyRepository: JourneyRepository,
    ): ImportDomainTimetablesUseCase = ImportDomainTimetablesUseCase(lineVersionRepository, journeyRepository)

    @Bean
    fun importDataTimetableUseCase(
        lineVersionJpaRepository: LineVersionJpaRepository,
        operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
        journeyJpaRepository: JourneyJpaRepository,
        scheduledStopJpaRepository: ScheduledStopJpaRepository,
    ): ImportDataTimetableUseCase = ImportDataTimetableUseCase(
        lineVersionJpaRepository,
        operatingPeriodJpaRepository,
        journeyJpaRepository,
        scheduledStopJpaRepository
    )
}