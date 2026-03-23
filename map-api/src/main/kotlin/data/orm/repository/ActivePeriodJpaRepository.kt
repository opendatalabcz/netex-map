package cz.cvut.fit.gaierda1.data.orm.repository

import cz.cvut.fit.gaierda1.data.orm.model.ActivePeriod
import org.springframework.data.jpa.repository.JpaRepository

interface ActivePeriodJpaRepository: JpaRepository<ActivePeriod, Long> {
}
