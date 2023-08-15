package searchApi.searching.features.concurrency

import api.SearchApi
import api.exception.BusySearchException
import api.exception.NoIndexSearchException
import api.tools.searchapi.index.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup

/**
 * Test situations when we start searching while index is working
 * */
class SearchWhileIndexingTest {

    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Search fails with exception BusySearchException if index for this folder is calculating
     * */
    @Test
    fun searchWhileIndexingSameFolderTest() {
        val folder = commonSetup.srcFolder
        val token = "index"
        val searchApi = searchApiGenerator()
        searchApi.createIndexAtFolder(folder)
        Assertions.assertThrows(
            BusySearchException::class.java,
            { searchApi.searchString(folder, token) },
            "Cannot search, during indexing process"
        )
    }

    /**
     * Search fails with exception BusySearchException if index for other folder is calculating
     * */
    @Test
    fun searchWhileIndexingInOtherFolderTest() {
        val folder1 = commonSetup.srcFolder.resolve("main")
        val folder2 = commonSetup.srcFolder.resolve("test")
        val token = "index"
        val searchApi = searchApiGenerator()
        searchApi.syncPerformIndex(folder1)
        searchApi.createIndexAtFolder(folder2)
        Assertions.assertThrows(
            BusySearchException::class.java,
            { searchApi.searchString(folder1, token) },
            "Cannot search, during indexing process"
        )
    }

    //Other instance
    /**
     * Search fails with exception NoIndexSearchException if index for this folder is calculating but in other SearchApi
     * So for current SearchApi there is no index and no indexing process
     * */
    @Test
    fun searchWhileIndexingSameFolderInOtherInstanceTest() {
        val folder = commonSetup.srcFolder
        val token = "index"
        val searchApi1 = searchApiGenerator()
        val searchApi2 = searchApiGenerator()
        searchApi1.createIndexAtFolder(folder)
        Assertions.assertThrows(
            NoIndexSearchException::class.java,
            { searchApi2.searchString(folder, token) },
            "Cannot search, during indexing process"
        )
    }

    /**
     * Search works normally if index for other folder is calculating in other instance.
     * */
    @Test
    fun searchWhileIndexingInOtherFolderInOtherInstanceTest() {
        val folder1 = commonSetup.srcFolder.resolve("main")
        val folder2 = commonSetup.srcFolder.resolve("test")
        val token = "index"
        val searchApi1 = searchApiGenerator()
        val searchApi2 = searchApiGenerator()
        searchApi1.syncPerformIndex(folder1)
        val indexingState2 = searchApi2.createIndexAtFolder(folder2)
        val searchingState = searchApi1.searchString(folder1, token)
        Assertions.assertAll(
            { Assertions.assertTrue(searchingState.result.get().isNotEmpty(), "Search result is not empty") },
            { Assertions.assertTrue(indexingState2.result.get().isNotEmpty(), "Index result is not empty") },
            { Assertions.assertTrue(searchApi2.hasIndexAtFolder(folder2), "SearchApi2 now has index for folder2") }
        )
    }
}