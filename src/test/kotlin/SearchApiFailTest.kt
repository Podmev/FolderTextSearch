import api.SearchApi
import api.exception.IllegalArgumentSearchException
import dummy.DummySearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.test.assertFailsWith

/*TODO add tests:
* File instead of folder
* \n test
* */

/*
* Set of unit test for checking search for invalid situations for all implementations
* Some tests use ready folders with files from test resources
*/
internal class SearchApiFailTest {
    private val projectPath: Path = Paths.get("")
    private val commonPath: Path =
        projectPath.resolve("src").resolve("test").resolve("resources").resolve("searchTestFolders")

    /*searching token with length 0, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token0LengthTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = ""
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { syncSearchToken(searchApi, folderPath, token) }
    }

    /*searching token with length 1, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token1LengthTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "a"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { syncSearchToken(searchApi, folderPath, token) }
    }

    /*searching token with length 2, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token2LengthTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "ab"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { syncSearchToken(searchApi, folderPath, token) }
    }

    companion object {
        private val dummySearchApi: SearchApi = DummySearchApi()

        /*list of implementations of SearchApi*/
        @JvmStatic
        fun searchApiProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(dummySearchApi),
                //TODO other implementations
            )
        }
    }
}