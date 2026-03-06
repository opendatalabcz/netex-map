package cz.cvut.fit.gaierda1.data.filesystem

import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import java.io.File
import java.io.InputStream

class TimetableDirectorySource(
    private val directory: File,
    private val fileCount: Int = Int.MAX_VALUE,
): TimetableSourcePort {
    init {
        if (!directory.isDirectory) throw IllegalArgumentException("Path ${directory.path} is not a directory")
    }

    override fun provideInput(): Sequence<InputStream> =
        directory.walkTopDown().filter { it.isFile }.take(fileCount).map(File::inputStream)
}
