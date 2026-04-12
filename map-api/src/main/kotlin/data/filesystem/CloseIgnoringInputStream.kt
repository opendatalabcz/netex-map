package cz.cvut.fit.gaierda1.data.filesystem

import java.io.FilterInputStream
import java.io.InputStream

class CloseIgnoringInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {
    override fun close() {
        // Do nothing
    }
}
