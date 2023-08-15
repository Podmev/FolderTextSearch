package searchApi.general

import api.SearchApi
import api.TokenMatch
import api.exception.IllegalArgumentSearchException
import api.exception.NotDirSearchException
import api.tools.searchapi.indexAndSearch.syncIndexAndSearchToken
import api.tools.searchapi.search.syncSearchTokenAfterIndex
import impl.indexless.IndexlessSearchApi
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.reflect.KFunction3
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
    @ParameterizedTest(name = "token0LengthTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun token0LengthTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = ""
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchToken(searchApi, folderPath, token)
        }
    }

    /**
     * Searching token with length 1, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "token1LengthTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun token1LengthTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchToken(searchApi, folderPath, token)
        }
    }

    /**
     * Searching token with length 2, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "token2LengthTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun token2LengthTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "ab"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchToken(searchApi, folderPath, token)
        }
    }

    /**
     * Searching token has symbol \n, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "tokenWithEscapeNTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun tokenWithEscapeNTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a\nb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchToken(searchApi, folderPath, token)
        }
    }

    /**
     * Searching token has symbol \r, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "tokenWithEscapeNTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun tokenWithEscapeRTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a\rb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchToken(searchApi, folderPath, token)
        }
    }

    /**
     * Searching token has 2 symbols \n\r, should be thrown IllegalArgumentSearchException.
     * */
    @ParameterizedTest(name = "tokenWithEscapeNEscapeRTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun tokenWithEscapeNEscapeRTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "a\n\rb"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(IllegalArgumentSearchException::class) {
            searchToken(searchApi, folderPath, token)
        }
    }

    /**
     * Folder path is actually file, should be thrown NotDirSearchException.
     * */
    @ParameterizedTest(name = "folderPathIsFileTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun folderPathIsFileTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "notFolder.txt"
        val token = "abc"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(NotDirSearchException::class) { searchToken(searchApi, folderPath, token) }
    }

    /**
     * Folder path is wrong - there is nothing on this way, should be thrown NotDirSearchException.
     * */
    @ParameterizedTest(name = "notExistingFolderTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun notExistingFolderTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "notExistingFolder"
        val token = "abc"
        val folderPath = commonPath.resolve(folderName)
        assertFailsWith(NotDirSearchException::class) { searchToken(searchApi, folderPath, token) }
    }

    companion object {
        private val indexlessSearchApiGenerator: () -> SearchApi = { IndexlessSearchApi() }
        private val trigramSearchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

        private val separateIndexAndSearchFunction: KFunction3<SearchApi, Path, String, List<TokenMatch>> =
            SearchApi::syncSearchTokenAfterIndex
        private val aggregatedIndexAndSearchFunction: KFunction3<SearchApi, Path, String, List<TokenMatch>> =
            SearchApi::syncIndexAndSearchToken

        /**
         * List of implementations of SearchApi
         * */
        @JvmStatic
        fun searchApiAndMethodProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(indexlessSearchApiGenerator, separateIndexAndSearchFunction, "syncSearchTokenAfterIndex"),
                Arguments.of(indexlessSearchApiGenerator, aggregatedIndexAndSearchFunction, "syncIndexAndSearchToken"),

                Arguments.of(trigramSearchApiGenerator, separateIndexAndSearchFunction, "syncSearchTokenAfterIndex"),
                Arguments.of(trigramSearchApiGenerator, aggregatedIndexAndSearchFunction, "syncIndexAndSearchToken")
            )
        }
    }
}