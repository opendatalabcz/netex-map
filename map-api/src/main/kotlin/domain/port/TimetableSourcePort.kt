package cz.cvut.fit.gaierda1.domain.port

import java.io.InputStream

interface TimetableSourcePort {
    fun provideInput(onEntry: (contentStream: InputStream) -> Unit)
}
