package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.domain.port.JourneyViewPort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperation
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ViewJourneys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.transaction.support.TransactionTemplate

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class ApplicationConfiguration {
    @Bean fun calculateNextDayOperation(): CalculateNextDayOperation = CalculateNextDayOperation()

    @Bean fun importTimetablesUseCase(
        journeyRepository: JourneyRepository,
        transactionTemplate: TransactionTemplate,
    ): ImportTimetablesUseCase = ImportTimetablesUseCaseTransactionWrapper(
        ImportTimetables(journeyRepository),
        transactionTemplate,
    )

    @Bean fun calculateJourneyRoutesMock(
        journeyRepository: JourneyRepository,
        transactionTemplate: TransactionTemplate,
    ): CalculateJourneyRoutesMock = CalculateJourneyRoutesMockTransactional(
        journeyRepository,
        transactionTemplate,
    )

    @Bean fun journeyViewPort(
        journeyRepository: JourneyRepository,
    ): JourneyViewPort = ViewJourneys(
        journeyRepository,
    )
}
