package cz.cvut.fit.gaierda1.domain.port

import java.io.InputStream

interface TimetableSourcePort {
    fun provideInput(): Sequence<InputStream>
}
