import api.SearchApi
import api.TokenMatch
import dummy.DummySearchApi
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
        val expectedResult = listOf(TokenMatch(folderPath.resolve("a.txt").toString(), 0L, 2L))
        val folderPathString = folderPath.toString()
        val searchApi = dummySearchApi
        val result = syncSearchToken(searchApi, folderPathString, token)
        assertEqualsTokenMatches(expectedResult, result)
    }

    @Test
    fun fileAndFolderWithFileTest() {
        val folderName = "fileAndFolderWithFile"
        val token = "cde"
        val folderPath = commonPath.resolve(folderName)
        val expectedResult = listOf(
            TokenMatch(folderPath.resolve("a.txt").toString(), 0L, 2L),
            TokenMatch(folderPath.resolve("b").resolve("c.txt").toString(), 0L, 0L)
        )
        val folderPathString = folderPath.toString()
        val searchApi = dummySearchApi
        val result = syncSearchToken(searchApi, folderPathString, token)
        assertEqualsTokenMatches(expectedResult, result)
    }


    private fun syncSearchToken(searchApi: SearchApi, folderPathString: String, token: String): List<TokenMatch> {
        val indexingState = searchApi.createIndexAtFolder(folderPathString)
        indexingState.result.get()!!
        assert(indexingState.finished)
        val searchingState = searchApi.searchString(folderPathString, token)
        return searchingState.result.get()
    }
}