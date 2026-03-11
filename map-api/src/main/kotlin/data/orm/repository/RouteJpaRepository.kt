package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.Route
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RouteJpaRepository: JpaRepository<Route, Long> {
    fun findByExternalId(externalId: String): Optional<Route>

    @Query("SELECT r FROM Journey j JOIN j.route r WHERE j.relationalId IN :journeyIds")
    fun findAllByJourney(journeyIds: List<Long>): List<Route>
}
