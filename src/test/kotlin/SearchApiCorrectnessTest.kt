import api.SearchApi
import api.TokenMatch
import dummy.DummySearchApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream


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