package cz.cvut.fit.gaierda1.config

import cz.cvut.fit.gaierda1.data.orm.repository.JourneyJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.LineVersionJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.OperatingPeriodJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.RouteStopJpaRepository
import cz.cvut.fit.gaierda1.data.orm.repository.ScheduledStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.JourneyViewPort
import cz.cvut.fit.gaierda1.domain.repository.JourneyRepository
import cz.cvut.fit.gaierda1.domain.repository.LineVersionRepository
import cz.cvut.fit.gaierda1.domain.repository.OperatingPeriodRepository
import cz.cvut.fit.gaierda1.domain.repository.RouteRepository
import cz.cvut.fit.gaierda1.domain.usecase.CalculateJourneyRoutesMock
import cz.cvut.fit.gaierda1.domain.usecase.CalculateNextDayOperation
import cz.cvut.fit.gaierda1.domain.usecase.data.ImportTimetablesData
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetables
import cz.cvut.fit.gaierda1.domain.usecase.ImportTimetablesUseCase
import cz.cvut.fit.gaierda1.domain.usecase.ViewJourneys
import cz.cvut.fit.gaierda1.domain.usecase.data.CalculateJourneyRoutesDataMock
import cz.cvut.fit.gaierda1.domain.usecase.data.CalculateNextDayOperationData
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class ApplicationConfiguration {
    @Bean fun calculateNextDayOperation(): CalculateNextDayOperation = CalculateNextDayOperation()

    @Bean fun calculateNextDayOperationData(): CalculateNextDayOperationData = CalculateNextDayOperationData()

    @Bean fun importTimetablesUseCase(
        lineVersionRepository: LineVersionRepository,
        operatingPeriodRepository: OperatingPeriodRepository,
        journeyRepository: JourneyRepository,
    ): ImportTimetablesUseCase = ImportTimetables(
        lineVersionRepository,
        operatingPeriodRepository,
        journeyRepository
    )

    @Bean fun importDataTimetables(
        lineVersionJpaRepository: LineVersionJpaRepository,
        operatingPeriodJpaRepository: OperatingPeriodJpaRepository,
        journeyJpaRepository: JourneyJpaRepository,
        scheduledStopJpaRepository: ScheduledStopJpaRepository,
    ): ImportTimetablesData = ImportTimetablesData(
        lineVersionJpaRepository,
        operatingPeriodJpaRepository,
        journeyJpaRepository,
        scheduledStopJpaRepository,
    )

    @Bean fun calculateJourneyRoutesMock(
        journeyRepository: JourneyRepository,
        routeRepository: RouteRepository,
    ): CalculateJourneyRoutesMock = CalculateJourneyRoutesMock(
        journeyRepository,
        routeRepository,
    )

    @Bean fun calculateJourneyRoutesDataMock(
        journeyJpaRepository: JourneyJpaRepository,
        routeJpaRepository: RouteJpaRepository,
        routeStopJpaRepository: RouteStopJpaRepository,
        physicalStopJpaRepository: PhysicalStopJpaRepository,
    ): CalculateJourneyRoutesDataMock = CalculateJourneyRoutesDataMock(
        journeyJpaRepository,
        routeJpaRepository,
        routeStopJpaRepository,
        physicalStopJpaRepository,
    )

    @Bean fun journeyViewPort(
        journeyRepository: JourneyRepository,
    ): JourneyViewPort = ViewJourneys(
        journeyRepository,
    )
}
