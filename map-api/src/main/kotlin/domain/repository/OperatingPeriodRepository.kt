package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod

interface OperatingPeriodRepository {
    fun saveAllIfAbsent(operatingPeriods: Iterable<OperatingPeriod>)
}