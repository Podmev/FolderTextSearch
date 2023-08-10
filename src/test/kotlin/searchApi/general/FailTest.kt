package searchApi.general

import api.SearchApi
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import api.tools.searchapi.syncSearchTokenAfterIndex
import impl.indexless.IndexlessSearchApi
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.assertFailsWith

/**
 * Set of unit test for checking search for invalid situations for all implementations
 * Some tests use ready folders with files from test resources
 * Using generator for SearchApi to have fresh state in SearchApi
 * */
internal class FailTest {
    private val commonPath: Path = commonSetup.commonPath

    /**
     * Searching token with length 0, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token0LengthTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = ""
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchApi.syncSearchTokenAfterIndex(
                folderPath,
                token
            )
        }
    }

    /**
     * Searching token with length 1, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token1LengthTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchApi.syncSearchTokenAfterIndex(
                folderPath,
                token
            )
        }
    }

    /**
     * Searching token with length 2, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun token2LengthTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "ab"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchApi.syncSearchTokenAfterIndex(
                folderPath,
                token
            )
        }
    }

    /**
     * Searching token has symbol \n, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tokenWithEscapeNTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a\nb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchApi.syncSearchTokenAfterIndex(
                folderPath,
                token
            )
        }
    }

    /**
     * Searching token has symbol \r, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tokenWithEscapeRTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a\rb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchApi.syncSearchTokenAfterIndex(
                folderPath,
                token
            )
        }
    }

    /**
     * Searching token has 2 symbols \n\r, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tokenWithEscapeNEscapeRTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a\n\rb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchApi.syncSearchTokenAfterIndex(
                folderPath,
                token
            )
        }
    }

    /**
     * Folder path is actually file, should be thrown NotDirSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun folderPathIsFileTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "notFolder.txt"
        val token = "abc"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(NotDirSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    /**
     * Folder path is wrong - there is nothing on this way, should be thrown NotDirSearchException.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun notExistingFolderTest(searchApiGenerator: () -> SearchApi) {
        val searchApi = searchApiGenerator()
        val folderName = "notExistingFolder"
        val token = "abc"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(NotDirSearchException::class) { searchApi.syncSearchTokenAfterIndex(folderPath, token) }
    }

    companion object {
        private val indexlessSearchApiGenerator: () -> SearchApi = { IndexlessSearchApi() }
        private val trigramSearchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

        /**
         * List of implementations of SearchApi
         * */
        @JvmStatic
        fun searchApiProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(indexlessSearchApiGenerator),
                Arguments.of(trigramSearchApiGenerator)
            )
        }
    }
}