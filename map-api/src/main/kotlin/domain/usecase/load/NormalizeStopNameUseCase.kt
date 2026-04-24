package cz.cvut.fit.gaierda1.domain.usecase.load

interface NormalizeStopNameUseCase {
    fun normalize(stopName: String): String
}
