package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.TariffStop
import cz.cvut.fit.gaierda1.data.orm.model.TariffStopId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TariffStopJpaRepository: JpaRepository<TariffStop, TariffStopId> {
}
