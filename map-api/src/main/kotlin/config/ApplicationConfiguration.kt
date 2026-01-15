package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.repository.TimetableStopRepository
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfiguration {
    @Bean
    fun importTimetablesUseCase(
        lineVersionRepository: LineVersionRepository,
        timetableStopRepository: TimetableStopRepository,
        journeyRepository: JourneyRepository,
    ): ImportTimetablesUseCase =
        ImportTimetablesUseCase(lineVersionRepository, timetableStopRepository, journeyRepository)
}