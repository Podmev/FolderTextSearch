import api.SearchApi
import api.TokenMatch
import dummy.DummySearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

/*TODO add tests:
* File instead of folder
* 0,1,2 symbols token test with exception
* Deep file, 10 level
* 10 files in one dir
* \n test
* Search  3 spaces
* Search "a a"
* Search """
* Empty folder
* Empty inner folder
* Test with natural test
* Long one line in file and in token
* Generator for folder heirarchy
* */

/*
* Set of unit test for checking search for correctness for all implementations
* Some tests use ready folders with files from test resources
*/
internal class SearchApiCorrectnessTest {
    private val projectPath: Path = Paths.get("")
    private val commonPath: Path =
        projectPath.resolve("src").resolve("test").resolve("resources").resolve("searchTestFolders")

    /*Folder with single file with single line, which contains token 1 time*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun singleFileTest(searchApi: SearchApi) {
        val folderName = "singleFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(TokenMatch(folderPath.resolve("a.txt"), 0L, 2L)),
            actualTokenMatches = syncSearchToken(searchApi, folderPath, token)
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
                TokenMatch(folderPath.resolve("a.txt"), 0L, 2L),
                TokenMatch(folderPath.resolve("b").resolve("c.txt"), 0L, 0L)
            ),
            actualTokenMatches = syncSearchToken(searchApi, folderPath, token)
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
                TokenMatch(folderPath.resolve("a.txt"), 0L, 0L),
                TokenMatch(folderPath.resolve("a.txt"), 0L, 1L),
                TokenMatch(folderPath.resolve("a.txt"), 0L, 2L),
                TokenMatch(folderPath.resolve("a.txt"), 0L, 3L)
            ),
            actualTokenMatches = syncSearchToken(searchApi, folderPath, token)
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
                TokenMatch(folderPath.resolve("a.txt"), 0L, 6L),
                TokenMatch(folderPath.resolve("a.txt"), 1L, 3L),
                TokenMatch(folderPath.resolve("a.txt"), 2L, 0L)
            ),
            actualTokenMatches = syncSearchToken(searchApi, folderPath, token)
        )
    }

    /*Folder with 2 files, only 1 has token*/
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun twoFilesOneMatch(searchApi: SearchApi) {
        val folderName = "twoFilesOneMatch"
        val token = "mnopq"
        val folderPath = commonPath.resolve(folderName)
        assertEqualsTokenMatches(
            expectedTokenMatches = listOf(
                TokenMatch(folderPath.resolve("b.txt"), 0L, 2L)
            ),
            actualTokenMatches = syncSearchToken(searchApi, folderPath, token)
        )
    }

    private fun syncSearchToken(searchApi: SearchApi, folderPathString: Path, token: String): List<TokenMatch> {
        val indexingState = searchApi.createIndexAtFolder(folderPathString)
        indexingState.result.get()!!
        assert(indexingState.finished)
        val searchingState = searchApi.searchString(folderPathString, token)
        return searchingState.result.get()
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