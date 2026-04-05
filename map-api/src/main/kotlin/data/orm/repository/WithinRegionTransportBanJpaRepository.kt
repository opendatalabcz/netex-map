package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.WithinRegionTransportBan
import cz.cvut.fit.gaierda1.data.orm.model.WithinRegionTransportBanId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WithinRegionTransportBanJpaRepository: JpaRepository<WithinRegionTransportBan, WithinRegionTransportBanId> {
}
