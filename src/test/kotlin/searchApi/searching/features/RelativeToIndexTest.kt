package searchApi.searching.features

import api.SearchApi
import api.exception.NoIndexSearchException
import api.tools.searchapi.syncPerformIndex
import api.tools.searchapi.syncPerformSearch
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup

/**
 * Checking relation between searching and indexing
 * */
class RelativeToIndexTest {
    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Search works if there is index exactly for this folder
     * */
    @Test
    fun searchInFolderWithIndexTest() {
        val folder = commonSetup.srcFolder
        val token = "index"
        val searchApi = searchApiGenerator()
        searchApi.syncPerformIndex(folder)
        val state = searchApi.searchString(folder, token)
        Assertions.assertTrue(state.result.get().isNotEmpty(),"Search gives results after indexing")
    }

    /**
     * Search throws NoIndexSearchException if there is no index exactly for this folder
     * */
    @Test
    fun searchInFolderWithoutIndexTest() {
        val folder = commonSetup.srcFolder
        val token = "index"
        val searchApi = searchApiGenerator()

        Assertions.assertThrows(
            /* expectedType = */ NoIndexSearchException::class.java,
            /* executable = */ { searchApi.syncPerformSearch(folder, token) },
            /* message = */ "No index for folder without index - throws NoIndexSearchException"
        )
    }

    /**
     * Search throws NoIndexSearchException if there is no index exactly for this folder,
     * even though there is index for parent folder
     * */
    @Test
    fun searchInSubfolderOfIndexedFolderTest() {
        val folder = commonSetup.srcFolder
        val subFolder = commonSetup.srcFolder.resolve("main")
        val token = "index"
        val searchApi = searchApiGenerator()
        searchApi.syncPerformIndex(folder)

        Assertions.assertThrows(
            /* expectedType = */ NoIndexSearchException::class.java,
            /* executable = */ { searchApi.syncPerformSearch(subFolder, token) },
            /* message = */ "No index for subfolder - throws NoIndexSearchException"
        )
    }
}