package searchApi.general

import api.SearchApi
import api.TokenMatch
import api.tools.searchapi.indexAndSearch.syncIndexAndSearchToken
import api.tools.searchapi.search.syncSearchTokenAfterIndex
import impl.indexless.IndexlessSearchApi
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.assertEqualsTokenMatches
import searchApi.common.commonSetup
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.reflect.KFunction3

/*TODO add tests:
* Empty folder - how to add to git? maybe generated
* Empty inner folder - how to add to git? maybe generated
* Test with natural test
* Test with all lyrics of Beatles songs, find word "love" there. It should be more 500 times
* Long one line in file and in token
* Generator for folder hierarchy
* */

/**
 * Set of unit test for checking search for correctness for all implementations
 * Some tests use ready folders with files from test resources
 *
 * Using generator for SearchApi to have fresh state in SearchApi
 * */
internal class CorrectnessTest {
    private val commonPath: Path = commonSetup.commonPath

    /**
     * Folder with single file with single line, which contains token 1 time.
     * */
    @ParameterizedTest(name = "singleFileTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun singleFileTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "singleFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(TokenMatch(folderPath.resolve("a.txt"), 1L, 3L)),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with file and inner folder with file. Both files have single line, which contains token 1 time.
     * */
    @ParameterizedTest(name = "fileAndFolderWithFileTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileAndFolderWithFileTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileAndFolderWithFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 3L),
                TokenMatch(folderPath.resolve("b").resolve("c.txt"), 1L, 1L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with single file with single line "aaaaaa", and we search "aaa", so it will be found there 4 times.
     * */
    @ParameterizedTest(name = "fileWith6LetterA_searchAAATest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileWith6LetterA_searchAAATest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileWith6LetterA"
        val token = "aaa"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 1L),
                TokenMatch(folderPath.resolve("a.txt"), 1L, 2L),
                TokenMatch(folderPath.resolve("a.txt"), 1L, 3L),
                TokenMatch(folderPath.resolve("a.txt"), 1L, 4L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with single file with 3 lines, and each one of them has token on different position.
     * */
    @ParameterizedTest(name = "fileWithMatchesOnDifferentLinesTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileWithMatchesOnDifferentLinesTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileWithMatchesOnDifferentLines"
        val token = "ghi"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 7L),
                TokenMatch(folderPath.resolve("a.txt"), 2L, 4L),
                TokenMatch(folderPath.resolve("a.txt"), 3L, 1L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with 2 files, only 1 has token.
     * */
    @ParameterizedTest(name = "twoFilesOneMatchTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun twoFilesOneMatchTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "twoFilesOneMatch"
        val token = "mnopq"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("b.txt"), 1L, 3L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with sequence of 10 inner folder with single file, which has 1 match.
     * */
    @ParameterizedTest(name = "deepFileTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun deepFileTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "deepFile"
        val token = "def"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(
                    filePath = folderPath
                        .resolve("1")
                        .resolve("2")
                        .resolve("3")
                        .resolve("4")
                        .resolve("5")
                        .resolve("6")
                        .resolve("7")
                        .resolve("8")
                        .resolve("9")
                        .resolve("10")
                        .resolve("a.txt"),
                    line = 1L, column = 4L
                )
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with 10 files, only 3 of them have match.
     * */
    @ParameterizedTest(name = "tenFilesAndHasMatchTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun tenFilesAndHasMatchTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "tenFiles"
        val token = "fgh"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("4.txt"), 1L, 3L),
                TokenMatch(folderPath.resolve("5.txt"), 1L, 2L),
                TokenMatch(folderPath.resolve("6.txt"), 1L, 1L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with file which has one time with 3 spaces ("   "), it will be 1 match.
     * */
    @ParameterizedTest(name = "fileWith3SpacesTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileWith3SpacesTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileWith3Spaces"
        val token = "   "
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 4L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with file which has one time with 3 doubleQuotes ("""), it will be 1 match
     * */
    @ParameterizedTest(name = "fileWith3DoubleQuotesTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileWith3DoubleQuotesTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileWith3DoubleQuotes"
        val token = "   "
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 4L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     * Folder with file which has 1 space between words "aaa", if we look for "a a" it will be 1 match.
     * */
    @ParameterizedTest(name = "fileWith1SpaceBetweenWordsTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileWith1SpaceBetweenWordsTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileWith1Space"
        val token = "a a"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 3L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
    }

    /**
     *Folder with emojis
     * Attention: emojis take 2 characters
     * */
    @ParameterizedTest(name = "fileWithEmojisTest{0} searchFunction:{2}")
    @MethodSource("searchApiAndMethodProvider")
    fun fileWithEmojisTest(
        searchApiGenerator: () -> SearchApi,
        searchToken: KFunction3<SearchApi, Path, String, List<TokenMatch>>,
        functionName: String
    ) {
        val searchApi = searchApiGenerator()
        val folderName = "fileWithEmojis"
        val token = "\uD83E\uDD51\uD83E\uDD66\uD83C\uDF49"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 7L)
            ),
            actualTokenMatches = searchToken(searchApi, folderPath, token)
        )
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