package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.OsmStopsServicePort
import cz.cvut.fit.gaierda1.domain.port.ServiceUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.support.TransactionTemplate

class ImportPhysicalStopsFromOsm(
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    @Value($$"${stops.service.health-check-tries}") private val healthCheckTries: Int,
    @Value($$"${stops.service.health-check-wait-ms}") private val healthCheckWaitMs: Long,
): ImportPhysicalStopsFromOsmUseCase {
    private val log = LoggerFactory.getLogger(ImportPhysicalStopsFromOsmUseCase::class.java)

    private fun getStopsWithTries(osmStopsServicePort: OsmStopsServicePort): List<PhysicalStop> {
        var healthFails = 0
        var physicalStops = emptyList<PhysicalStop>()
        do {
            try {
                physicalStops = osmStopsServicePort.getPhysicalStops()
                break
            } catch (e: ServiceUnavailableException) {
                ++healthFails
                log.info("OSM service is not available, retrying in $healthCheckWaitMs ms. Reason: ${e.message}")
                Thread.sleep(healthCheckWaitMs)
            }
        } while (healthFails < healthCheckTries)
        if (physicalStops.isEmpty()) {
            error("OSM service is not available")
        }
        return physicalStops
    }

    override fun importPhysicalStopsFromOsm(osmStopsServicePort: OsmStopsServicePort) {
        val physicalStops = getStopsWithTries(osmStopsServicePort)
        transactionTemplate.executeWithoutResult {
            for (physicalStop in physicalStops) {
                val stopId = physicalStopJpaRepository.findIdByExternalId(physicalStop.externalId)
                physicalStop.relationalId = stopId.orElse(null)
            }
            physicalStopJpaRepository.saveAll(physicalStops)
        }
    }
}
