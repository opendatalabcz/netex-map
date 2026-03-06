package cz.cvut.fit.gaierda1.domain.repository

import cz.cvut.fit.gaierda1.domain.model.OperatingPeriod

interface OperatingPeriodRepository {
    fun saveIfAbsent(operatingPeriod: OperatingPeriod)
    fun saveAllIfAbsent(operatingPeriods: Iterable<OperatingPeriod>)
}