package cz.cvut.fit.gaierda1.domain.port

import java.io.InputStream

interface JrUtilGtfsSourcePort {
    enum class GtfsFileType {
        LINES,
        JOURNEYS,
        SCHEDULED_STOPS,
        STOPS,
    }
    data class GtfsInputStream(
        val type: GtfsFileType,
        val inputStream: InputStream,
    )
    fun provideInput(): Sequence<GtfsInputStream>
}
