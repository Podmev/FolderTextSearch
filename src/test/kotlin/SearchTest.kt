import api.FileMatch
import api.SearchApi
import api.SearchResult
import api.TokenMatch
import dummy.DummySearchApi
import dummy.DummySearchResult
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

internal class SearchTest {
    private val projectPath: Path = Paths.get("")
    private val commonPath: Path =
        projectPath.resolve("src").resolve("test").resolve("resources").resolve("searchTestFolders")

    private val dummySearchApi: SearchApi = DummySearchApi()

    @Test
    fun singleFileTest() {
        val folderName = "singleFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)

        val expectedResult = DummySearchResult(
            fileMatches = listOf(
                FileMatch(
                    filePath = folderPath.resolve("a.txt").toString(), tokenMatches = listOf(TokenMatch(0L, 2L))
                )
            ), totalTokenMatches = 1
        )
        val folderPathString = folderPath.toString()
        val searchApi = dummySearchApi
        val result = syncSearchToken(searchApi, folderPathString, token)
        assertEqualsSearchResults(expectedResult, result)
    }

    @Test
    fun fileAndFolderWithFileTest() {
        val folderName = "fileAndFolderWithFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)

        val expectedResult = DummySearchResult(
            fileMatches = listOf(
                FileMatch(
                    filePath = folderPath.resolve("a.txt").toString(), tokenMatches = listOf(TokenMatch(0L, 2L))
                ), FileMatch(
                    filePath = folderPath.resolve("b").resolve("c.txt").toString(),
                    tokenMatches = listOf(TokenMatch(0L, 0L))
                )
            ), totalTokenMatches = 2
        )
        val folderPathString = folderPath.toString()
        val searchApi = dummySearchApi
        val result = syncSearchToken(searchApi, folderPathString, token)
        assertEqualsSearchResults(expectedResult, result)
    }


    private fun syncSearchToken(searchApi: SearchApi, folderPathString: String, token: String): SearchResult {
        val indexingState = searchApi.createIndexAtFolder(folderPathString)
        while (!indexingState.isFinished()) {
            Thread.sleep(10)
        }
        val searchingState = searchApi.searchString(folderPathString, token)

        while (!searchingState.isFinished()) {
            Thread.sleep(10)
        }
        return searchingState.getResult()
    }
}