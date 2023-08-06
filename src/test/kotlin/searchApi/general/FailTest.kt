package searchApi.general

import api.SearchApi
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import searchApi.common.commonSetup
import api.tools.syncSearchTokenAfterIndex
import impl.indexless.IndexlessSearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import impl.trigram.TrigramSearchApi
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.assertFailsWith

/*
* Set of unit test for checking search for invalid situations for all implementations
* Some tests use ready folders with files from test resources
*/
internal class FailTest {
    private val commonPath: Path = commonSetup.commonPath

    /*searching token with length 0, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token0LengthTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = ""
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*searching token with length 1, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token1LengthTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "a"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*searching token with length 2, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token2LengthTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "ab"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*searching token has symbol \n, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tokenWithEscapeNTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "a\nb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*searching token has symbol \r, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tokenWithEscapeRTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "a\rb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*searching token has 2 symbols \n\r, should be thrown IllegalArgumentSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tokenWithEscapeNEscapeRTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "a\n\rb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*folder path is actually file, should be thrown NotDirSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun folderPathIsFileTest(searchApi: SearchApi) {
        val folderName = "notFolder.txt"
        val token = "abc"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(NotDirSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /*folder path is wrong - there is nothing on this way, should be thrown NotDirSearchException*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun notExistingFolderTest(searchApi: SearchApi) {
        val folderName = "notExistingFolder"
        val token = "abc"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(NotDirSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    companion object {
        private val indexlessSearchApi: SearchApi = IndexlessSearchApi()
        private val trigramSearchApi: SearchApi = TrigramSearchApi()

        /*list of implementations of SearchApi*/
        @JvmStatic
        fun searchApiProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(indexlessSearchApi),
                Arguments.of(trigramSearchApi)
            )
        }
    }
}