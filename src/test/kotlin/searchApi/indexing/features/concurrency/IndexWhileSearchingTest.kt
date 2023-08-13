package searchApi.indexing.features.concurrency

import api.SearchApi
import api.SearchingState
import api.tools.searchapi.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup

class IndexWhileSearchingTest {

    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Index can start and finish normally, while searches
     * */
    @Test
    fun indexWhileSearchTest() {
        val folder1 = commonSetup.srcFolder.resolve("main")
        val folder2 = commonSetup.srcFolder.resolve("test")
        val token = "index"
        val searchApi = searchApiGenerator()
        searchApi.syncPerformIndex(folder1)
        val searchingState: SearchingState = searchApi.searchString(folder1, token)
        val indexingState = searchApi.createIndexAtFolder(folder2)
        Assertions.assertAll(
            { Assertions.assertTrue(searchingState.result.get().isNotEmpty(), "Search result is not empty") },
            { Assertions.assertTrue(indexingState.result.get().isNotEmpty(), "Index result is not empty") },
            { Assertions.assertTrue(searchApi.hasIndexAtFolder(folder2), "SearchApi now has index for folder2") }
        )
    }
}