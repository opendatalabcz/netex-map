package cz.cvut.fit.gaierda1.data.filesystem

import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsSourcePort
import cz.cvut.fit.gaierda1.domain.port.JrUtilGtfsSourcePort.*
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class JrUtilGtfsZipFileSource(
    private val zipFile: InputStream,
): JrUtilGtfsSourcePort {
    override fun provideInput(): Sequence<GtfsInputStream> {
        return sequence {
            ZipInputStream(zipFile).use { zipInputStream ->
                var entry: ZipEntry? = zipInputStream.nextEntry
                while (entry != null) {
                    if (entry.isDirectory) {
                        entry = zipInputStream.nextEntry
                        continue
                    }
                    when (entry.name) {
                        "routes.txt" -> {
                            yield(GtfsInputStream(
                                type = GtfsFileType.LINES,
                                inputStream = CloseIgnoringInputStream(zipInputStream)
                            ))
                        }
                        "trips.txt" -> {
                            yield(GtfsInputStream(
                                type = GtfsFileType.JOURNEYS,
                                inputStream = CloseIgnoringInputStream(zipInputStream)
                            ))
                        }
                        "stop_times.txt" -> {
                            yield(GtfsInputStream(
                                type = GtfsFileType.SCHEDULED_STOPS,
                                inputStream = CloseIgnoringInputStream(zipInputStream)
                            ))
                        }
                        "stops.txt" -> {
                            yield(GtfsInputStream(
                                type = GtfsFileType.STOPS,
                                inputStream = CloseIgnoringInputStream(zipInputStream)
                            ))
                        }
                        else -> {}
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        }
    }
}
