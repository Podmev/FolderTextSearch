package searchApi.indexing.features

import api.IndexingState
import api.isIndexEmpty
import api.tools.searchapi.syncPerformIndex
import api.tools.state.asyncCancelAtProgress
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.CommonSetup
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Checks correctness of cancel of indexing in SearchApi
 * */
@Suppress("DeferredResultUnused")
class CancelTest {
    /**
     * Source code of current project.
     * */
    private val commonPath: Path = CommonSetup.srcFolder

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi.
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Cancel at the start indexing.
     * Code shouldn't throw any exception, it should be saved no index.
     * */
    @Test
    fun curProjectIndexingAndCancelAtStartIndexingTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val previouslyFinishedIndexingState = searchApi.syncPerformIndex(folder)
        searchApi.removeFullIndex()
        /*total should exist* */
        val completedTotalFilesNumber = previouslyFinishedIndexingState.totalFilesNumber!!

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        state.asyncCancelAtProgress(
            cancelAtProgress = 0.0,
            checkProgressEveryMillis = 1
        )
        state.result.get()!!
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.isIndexEmpty(), "SearchApi has no index") },
            {
                Assertions.assertTrue(
                    state.visitedFilesNumber < completedTotalFilesNumber,
                    "visited < total(precalculated)"
                )
            },
            { Assertions.assertEquals(0L, state.indexedFilesNumber, "indexedFilesNumber == 0") },
            { Assertions.assertEquals(null, state.totalFilesNumber, "totalFilesNumber == null") },
            { Assertions.assertEquals(1.0, state.progress, "progress == 1.0") },
        )
    }

    /**
     * Cancel during indexing at progress cancelAtProgress.
     * Code shouldn't throw any exception, it should be saved no index.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("progressProvider")
    fun curProjectIndexingAndCancelDuringIndexTest(cancelAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val previouslyFinishedIndexingState = searchApi.syncPerformIndex(folder)
        searchApi.removeFullIndex()
        /*total should exist* */
        val completedTotalFilesNumber = previouslyFinishedIndexingState.totalFilesNumber!!

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        state.asyncCancelAtProgress(
            cancelAtProgress = cancelAtProgress,
            checkProgressEveryMillis = 5
        )
        state.result.get()!!
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.isIndexEmpty(), "SearchApi has no index") },
            {
                Assertions.assertTrue(
                    state.visitedFilesNumber <= completedTotalFilesNumber,
                    "visited <= total(precalculated)"
                )
            },
            { Assertions.assertTrue(state.indexedFilesNumber > 0L, "indexedFilesNumber > 0") },
            { Assertions.assertEquals(1.0, state.progress, "progress == 1.0") }, //FIXME this logic
        )
        //totalFilesNumber can be null or defined. Cannot know by progress
    }

    /**
     * Cancel after indexing.
     * Code shouldn't throw any exception, it should be saved index for folder.
     * visited files number, indexed files number and total should be equal.
     * */
    @Test
    fun curProjectIndexingAndCancelAfterIndexTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val state: IndexingState = searchApi.syncPerformIndex(folder)
        state.cancel()
        /*total should exist* */
        val totalFilesNumber = state.totalFilesNumber!!
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.hasIndexAtFolder(folder), "SearchApi has index at folder") },
            { Assertions.assertEquals(totalFilesNumber, state.visitedFilesNumber, "visited == total") },
            { Assertions.assertEquals(totalFilesNumber, state.indexedFilesNumber, "indexed == total") },
            { Assertions.assertEquals(1.0, state.progress, "progress == 1.0") },
        )
    }

    companion object {
        /**
         * Values for progress, which are not 0.0 and 1.0. Used for parametrized test.
         * */
        private val inMiddleProgressList = listOf(
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9
        )

        /**
         * Provides arguments for tests: progress
         * */
        @JvmStatic
        fun progressProvider(): Stream<Arguments> {
            return inMiddleProgressList
                .map { Arguments.of(it) }
                .stream()
        }
    }
}