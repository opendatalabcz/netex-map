package cz.cvut.fit.gaierda1.data.filesystem

import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import java.io.File
import java.io.InputStream

class TimetableDirectorySource(
    private val directory: File,
): TimetableSourcePort {
    init {
        if (!directory.isDirectory) throw IllegalArgumentException("Path ${directory.path} is not a directory")
    }

    override fun provideInput(onEntry: (contentStream: InputStream) -> Unit) {
        for (file in directory.walkTopDown().filter { it.isFile }) {
            file.inputStream().use(onEntry)
        }
    }
}
