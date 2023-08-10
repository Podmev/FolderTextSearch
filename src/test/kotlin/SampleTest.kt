import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test, that should always work.
 * It can be used for checking CI/CD
 * */
internal class SampleTest {
    private val testSample: Sample = Sample()

    @Test
    fun testSum() {
        val expected = 43 //Wrong for testing CI/CD
        assertEquals(expected, testSample.sum(40, 2))
    }
}