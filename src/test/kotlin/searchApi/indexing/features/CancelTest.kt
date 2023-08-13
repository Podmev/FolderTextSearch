package searchApi.indexing.features

import api.IndexingState
import api.ProgressableStatus
import api.isIndexEmpty
import api.tools.searchapi.syncPerformIndex
import api.tools.state.asyncCancelAtProgress
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
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
    private val commonPath: Path = commonSetup.srcFolder

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi.
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Cancel at the start indexing.
     * Code shouldn't throw any exception, it should be saved no index.
     * */
    @Test
    fun indexingAndCancelAtStartIndexingTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val previouslyFinishedIndexingState = searchApi.syncPerformIndex(folder)
        searchApi.removeFullIndex()
        /*total should exist* */
        val completedTotalFilesNumber = previouslyFinishedIndexingState.totalFilesNumber!!

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        state.asyncCancelAtProgress(
            cancelAtProgress = 0.0,
            checkProgressEveryMillis = 0
        )
        state.result.get()
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
            { Assertions.assertEquals(0.0, state.progress, "progress == 0.0") },
            { Assertions.assertEquals(ProgressableStatus.CANCELLED, state.status, "status == CANCELLED") },
        )
    }

    /**
     * Cancel during indexing at progress cancelAtProgress.
     * Code shouldn't throw any exception, it should be saved no index.
     * */
    @ParameterizedTest(name = "indexingAndCancelDuringIndexTest{0}")
    @MethodSource("progressProvider")
    fun indexingAndCancelDuringIndexTest(cancelAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val previouslyFinishedIndexingState = searchApi.syncPerformIndex(folder)
        searchApi.removeFullIndex()
        /*total should exist* */
        val completedTotalFilesNumber = previouslyFinishedIndexingState.totalFilesNumber!!

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        state.asyncCancelAtProgress(
            cancelAtProgress = cancelAtProgress,
            checkProgressEveryMillis = 0
        )
        state.result.get()
        val progress = state.progress
        Assertions.assertAll(
            { Assertions.assertTrue(searchApi.isIndexEmpty(), "SearchApi has no index") },
            {
                Assertions.assertTrue(
                    state.visitedFilesNumber <= completedTotalFilesNumber,
                    "visited <= total(precalculated)"
                )
            },
            { Assertions.assertTrue(state.indexedFilesNumber > 0L, "indexedFilesNumber > 0") },
            { Assertions.assertTrue(progress >= cancelAtProgress, "progress >= cancelAtProgress") },
            { Assertions.assertTrue(progress < 1.0, "progress < 1.0") },
            //In CI/CD doesn't work well, but locally works
            //{ Assertions.assertTrue(progress < cancelAtProgress + 0.1, "progress ($progress) < cancelAtProgress + 0.1") },
            { Assertions.assertEquals(ProgressableStatus.CANCELLED, state.status, "status == CANCELLED") },
            { Assertions.assertNotNull(state.totalFilesNumber, "totalFilesNumber is not null") },
        )
    }

    /**
     * Cancel after indexing.
     * Code shouldn't throw any exception, it should be saved index for folder.
     * visited files number, indexed files number and total should be equal.
     * */
    @Test
    fun indexingAndCancelAfterIndexTest() {
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
            { Assertions.assertEquals(ProgressableStatus.FINISHED, state.status, "status == FINISHED") },
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