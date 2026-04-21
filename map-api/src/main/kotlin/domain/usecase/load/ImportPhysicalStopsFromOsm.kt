package cz.cvut.fit.gaierda1.domain.usecase.load

import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.data.orm.repository.PhysicalStopJpaRepository
import cz.cvut.fit.gaierda1.domain.port.OsmParserPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.io.File

@Component
class ImportPhysicalStopsFromOsm(
    private val physicalStopJpaRepository: PhysicalStopJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    @Value($$"${import.physical-stops-batch-size}")
    private val physicalStopBatchSize: Int,
): ImportPhysicalStopsFromOsmUseCase {
    private val log = LoggerFactory.getLogger(ImportPhysicalStopsFromOsm::class.java)

    override fun importPhysicalStopsFromOsm(
        osmFile: File,
        osmParserPort: OsmParserPort,
    ) {
        val physicalStops = osmParserPort.parseOsmFile(osmFile)
        var newStopCount = 0
        var updatedStopCount = 0
        for (physicalStopsBatch in physicalStops.chunked(physicalStopBatchSize)) {
            transactionTemplate.executeWithoutResult {
                val externalIds = physicalStopsBatch.map { it.externalId }
                val existingStopsMap = physicalStopJpaRepository
                    .findAllByExternalIds(externalIds)
                    .associateBy(PhysicalStop::externalId)
                val newPhysicalStops = mutableListOf<PhysicalStop>()
                val updatedPhysicalStops = mutableListOf<PhysicalStop>()
                for (physicalStop in physicalStopsBatch) {
                    val existingStop = existingStopsMap[physicalStop.externalId]
                    if (existingStop != null) {
                        physicalStop.relationalId = existingStop.relationalId
                        updatedPhysicalStops.add(physicalStop)
                    } else {
                        physicalStop.relationalId = null
                        newPhysicalStops.add(physicalStop)
                    }
                }
                newStopCount += newPhysicalStops.size
                updatedStopCount += updatedPhysicalStops.size
                physicalStopJpaRepository.saveAll(updatedPhysicalStops)
                physicalStopJpaRepository.saveAll(newPhysicalStops)
            }
        }
        log.info("Imported $newStopCount new and updated $updatedStopCount existing physical stops.")
    }
}
