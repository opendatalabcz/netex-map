package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.JourneyViewPort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.repository.OperatingPeriodRepository
import cz.cvut.fit.gaierda1.domain.usecase.AssignRouteMock
import cz.cvut.fit.gaierda1.domain.usecase.ImportDataTimetableUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ImportDomainTimetablesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ViewJourneys
import cz.cvut.fit.gaierda1.domain.usecase.ViewJourneysWithMockedRoute
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class ApplicationConfiguration {
    @Bean
    fun importDomainTimetablesUseCase(
        lineVersionRepository: LineVersionRepository,
        operatingPeriodRepository: OperatingPeriodRepository,
        journeyRepository: JourneyRepository,
    ): ImportDomainTimetablesUseCase = ImportDomainTimetablesUseCase(
        lineVersionRepository,
        operatingPeriodRepository,
        journeyRepository,
    )

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
        scheduledStopJpaRepository,
    )

//    @Bean
//    fun viewJourneys(
//        journeyRepository: JourneyRepository,
//    ): JourneyViewPort = ViewJourneys(
//        journeyRepository,
//    )

    @Bean
    fun assignRouteMock(): AssignRouteMock = AssignRouteMock()

    @Bean
    fun viewJourneysWithMockedRoute(
        journeyRepository: JourneyRepository,
        assignRouteMock: AssignRouteMock,
    ): JourneyViewPort = ViewJourneysWithMockedRoute(
        journeyRepository,
        assignRouteMock,
    )
}