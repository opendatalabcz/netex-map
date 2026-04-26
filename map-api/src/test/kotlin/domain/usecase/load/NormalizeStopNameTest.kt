package cz.cvut.fit.gaierda1.domain.usecase.load

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class NormalizeStopNameTest {
    private lateinit var sut: NormalizeStopName

    @BeforeEach
    fun setup() {
        sut = NormalizeStopName()
    }

    @Test
    fun `should remove extra spaces around commas`() {
        val input = "Stop ,  \t Name"
        val normalized = sut.normalize(input)
        assertEquals("Stop,Name", normalized)
    }

    @Test
    fun `should collapse multiple neighbouring commas`() {
        val input = "Stop,,,Name"
        val normalized = sut.normalize(input)
        assertEquals("Stop,Name", normalized)
    }

    @Test
    fun `should collapse commas if separated only by whitespace`() {
        val input = "Stop,\t , ,Name"
        val normalized = sut.normalize(input)
        assertEquals("Stop,Name", normalized)
    }

    @Test
    fun `should remove square brackets and their contents at the end of a string`() {
        val input = "Territory [info]"
        val normalized = sut.normalize(input)
        assertEquals("Territory", normalized)
    }

    @Test
    fun `should remove square brackets and their contents in the middle of a string`() {
        val input = "Territory [info] Stop"
        val normalized = sut.normalize(input)
        assertEquals("Territory Stop", normalized)
    }

    @Test
    fun `should remove square brackets and their contents before a comma`() {
        val input = "Territory [info],Stop"
        val normalized = sut.normalize(input)
        assertEquals("Territory,Stop", normalized)
    }

    @Test
    fun `should put a space after a dot if not followed by a comma`() {
        val input = "Location,St.Name"
        val normalized = sut.normalize(input)
        assertEquals("Location,St. Name", normalized)
    }

    @Test
    fun `should not put a space after a dot if at the end of a string`() {
        val input = "Location,Bus st."
        val normalized = sut.normalize(input)
        assertEquals(input, normalized)
    }

    @Test
    fun `should not put a space after a dot if followed by a comma`() {
        val input = "Location at tr.,River side,Bus"
        val normalized = sut.normalize(input)
        assertEquals(input, normalized)
    }

    @Test
    fun `should remove a comma at the end of a string`() {
        val input = "Stop Name,"
        val normalized = sut.normalize(input)
        assertEquals("Stop Name", normalized)
    }
}
