package searchApi.indexing.features

import api.SearchApi
import api.tools.searchapi.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup

/**
 * Checking how cache works on indexing requests.
 * Tests show that there is cache.
 * */
class CachingTest {
    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Comparing time for first and second indexing the same folder.
     * Second almost doesn't take any time - uses cache.
     * */
    @Test
    fun repeatIndexHasCacheTest() {
        val folder = commonSetup.srcFolder
        val searchApi = searchApiGenerator()
        val state1 = searchApi.syncPerformIndex(folder)
        val state2 = searchApi.syncPerformIndex(folder)
        Assertions.assertAll(
            { Assertions.assertTrue(state1.totalTime > 300, "At first there is no cache, so index works longer") },
            { Assertions.assertTrue(state2.totalTime < 5, "Repeat index works fast - it uses cache") }
        )
    }
}