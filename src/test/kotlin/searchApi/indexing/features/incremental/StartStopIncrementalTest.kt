package searchApi.indexing.features.incremental

import api.SearchApi
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.util.concurrent.TimeUnit

/**
 * Checking starting and stopping incremental indexing
 * */
class StartStopIncrementalTest {
    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * Checking without any indexing, just start and stop
     * */
    @Test
    fun noFoldersStartStopTest() {
        val searchApi = searchApiGenerator()
        val started = searchApi.startIncrementalIndexing()
        TimeUnit.MILLISECONDS.sleep(500)
        val stopped = searchApi.stopIncrementalIndexing()
        assertAll(
            { Assertions.assertTrue(started, "Can start incremental indexing") },
            { Assertions.assertTrue(stopped, "Can stop incremental indexing") }
        )
    }

    /**
     * Checking without any indexing, 2 starts incremental indexing - second returns false - cannot start
     * */
    @Test
    fun noFoldersStartStartTest() {
        val searchApi = searchApiGenerator()
        val started1 = searchApi.startIncrementalIndexing()
        TimeUnit.MILLISECONDS.sleep(500)
        val started2 = searchApi.startIncrementalIndexing()
        assertAll(
            { Assertions.assertTrue(started1, "Can start incremental indexing") },
            { Assertions.assertFalse(started2, "Cannot start incremental indexing again") }
        )
    }

    /**
     * Checking without any indexing, just stop returns false, because incremental indexing is not in process
     * */
    @Test
    fun noFoldersStopTest() {
        val searchApi = searchApiGenerator()
        val stopped = searchApi.stopIncrementalIndexing()
        assertAll(
            { Assertions.assertFalse(stopped, "Cannot stop incremental indexing, when it is not working") }
        )
    }

    /**
     * Checking without any indexing, just start and stop, and then stop again. Last stop returns false
     * */
    @Test
    fun noFoldersStartStopStopTest() {
        val searchApi = searchApiGenerator()
        val started = searchApi.startIncrementalIndexing()
        TimeUnit.MILLISECONDS.sleep(500)
        val stopped1 = searchApi.stopIncrementalIndexing()
        val stopped2 = searchApi.stopIncrementalIndexing()
        assertAll(
            { Assertions.assertTrue(started, "Can start incremental indexing") },
            { Assertions.assertTrue(stopped1, "Can stop incremental indexing") },
            { Assertions.assertFalse(stopped2, "Cannot stop incremental indexing after stopping") },
        )
    }

}