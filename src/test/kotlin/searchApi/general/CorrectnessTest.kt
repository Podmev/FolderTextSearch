package searchApi.general

import api.SearchApi
import api.TokenMatch
import common.assertEqualsTokenMatches
import common.commonSetup
import api.tools.syncSearchTokenAfterIndex
import impl.indexless.IndexlessSearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import impl.trigram.TrigramSearchApi
import java.nio.file.Path
import java.util.stream.Stream

/*TODO add tests:
* Empty folder - how to add to git? maybe generated
* Empty inner folder - how to add to git? maybe generated
* Test with natural test
* Test with all lyrics of Beatles songs, find word "love" there. It should be more 500 times
* Long one line in file and in token
* Generator for folder hierarchy
* */

/*
* Set of unit test for checking search for correctness for all implementations
* Some tests use ready folders with files from test resources
*/
internal class CorrectnessTest {
    private val commonPath: Path = commonSetup.commonPath

    /*Folder with single file with single line, which contains token 1 time*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun singleFileTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(TokenMatch(folderPath.resolve("a.txt"), 1L, 3L)),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with file and inner folder with file. Both files have single line, which contains token 1 time*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileAndFolderWithFileTest(searchApi: SearchApi) {
        val folderName = "fileAndFolderWithFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 3L),
                TokenMatch(folderPath.resolve("b").resolve("c.txt"), 1L, 1L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with single file with single line "aaaaaa", and we search "aaa", so it will be found there 4 times*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileWith6LetterA_searchAAATest(searchApi: SearchApi) {
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
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with single file with 3 lines, and each one of them has token on different position*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileWithMatchesOnDifferentLinesTest(searchApi: SearchApi) {
        val folderName = "fileWithMatchesOnDifferentLines"
        val token = "ghi"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 7L),
                TokenMatch(folderPath.resolve("a.txt"), 2L, 4L),
                TokenMatch(folderPath.resolve("a.txt"), 3L, 1L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with 2 files, only 1 has token*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun twoFilesOneMatchTest(searchApi: SearchApi) {
        val folderName = "twoFilesOneMatch"
        val token = "mnopq"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("b.txt"), 1L, 3L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with sequence of 10 inner folder with single file, which has 1 match*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun deepFileTest(searchApi: SearchApi) {
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
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with 10 files, only 3 of them have match*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun tenFilesAndHasMatchTest(searchApi: SearchApi) {
        val folderName = "tenFiles"
        val token = "fgh"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("4.txt"), 1L, 3L),
                TokenMatch(folderPath.resolve("5.txt"), 1L, 2L),
                TokenMatch(folderPath.resolve("6.txt"), 1L, 1L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with file which has one time with 3 spaces ("   "), it will be 1 match*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileWith3SpacesTest(searchApi: SearchApi) {
        val folderName = "fileWith3Spaces"
        val token = "   "
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 4L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with file which has one time with 3 doubleQuotes ("""), it will be 1 match*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileWith3DoubleQuotesTest(searchApi: SearchApi) {
        val folderName = "fileWith3DoubleQuotes"
        val token = "   "
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 4L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with file which has 1 space between words "aaa", if we look for "a a" it will be 1 match*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileWith1SpaceBetweenWordsTest(searchApi: SearchApi) {
        val folderName = "fileWith1Space"
        val token = "a a"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 3L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
    }

    /*Folder with emojis
    * Attention: emojis take 2 characters
    * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun fileWithEmojisTest(searchApi: SearchApi) {
        val folderName = "fileWithEmojis"
        val token = "\uD83E\uDD51\uD83E\uDD66\uD83C\uDF49"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("a.txt"), 1L, 7L)
            ),
            actualTokenMatches = searchApi.syncSearchTokenAfterIndex(folderPath, token)
        )
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