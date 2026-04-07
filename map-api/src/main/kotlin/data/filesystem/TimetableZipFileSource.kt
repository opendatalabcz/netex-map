package cz.cvut.fit.gaierda1.data.filesystem

import cz.cvut.fit.gaierda1.domain.port.TimetableSourcePort
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry

class TimetableZipFileSource(
    private val zipFile: InputStream,
    private val fileCount: Int = Int.MAX_VALUE,
): TimetableSourcePort {
    override fun provideInput(): Sequence<InputStream> {
        return sequence {
            ZipInputStream(zipFile).use { zipInputStream ->
                var entry: ZipEntry? = zipInputStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val streamContent = zipInputStream.readBytes()
                        yield(ByteArrayInputStream(streamContent))
                        zipInputStream.closeEntry()
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        }.take(fileCount)
    }
}