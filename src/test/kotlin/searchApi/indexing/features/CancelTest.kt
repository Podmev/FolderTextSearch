package searchApi.indexing.features

import api.IndexingState
import api.isIndexEmpty
import api.tools.syncPerformIndex
import impl.trigram.TrigramSearchApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import java.nio.file.Path
import java.util.stream.Stream

/* Checks correctness of cancel of indexing in SearchApi
* */
class CancelTest {
    /*source code of intellij idea* */
    private val commonPath: Path = commonSetup.srcFolder

    /*using not by interface, because we use methods exactly from TrigramSearchApi* */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /*Cancel at the start indexing.
    * Code shouldn't throw any exception, it should be saved no index
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
        asyncCancelAtProgress(
            indexingState = state,
            cancelAtProgress = 0.0,
            checkProgressEveryMillis = 1
        )
        state.result.get()!!
        Assertions.assertAll(
            { -> Assertions.assertTrue(searchApi.isIndexEmpty(), "SearchApi has no index") },
            { -> Assertions.assertTrue(state.visitedFilesNumber < completedTotalFilesNumber, "visited < total(precalculated)") },
            { -> Assertions.assertEquals(0L, state.indexedFilesNumber, "indexedFilesNumber == 0") },
            { -> Assertions.assertEquals(null, state.totalFilesNumber, "totalFilesNumber == null") },
            { -> Assertions.assertEquals(1.0, state.progress, "progress == 1.0") },
        )
    }

    /*Cancel during indexing at progress cancelAtProgress.
    * Code shouldn't throw any exception, it should be saved no index
    * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun curProjectIndexingAndCancelDuringIndexTest(cancelAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val previouslyFinishedIndexingState = searchApi.syncPerformIndex(folder)
        searchApi.removeFullIndex()
        /*total should exist* */
        val completedTotalFilesNumber = previouslyFinishedIndexingState.totalFilesNumber!!

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        asyncCancelAtProgress(
            indexingState = state,
            cancelAtProgress = cancelAtProgress,
            checkProgressEveryMillis = 5
        )
        state.result.get()!!
        Assertions.assertAll(
            { -> Assertions.assertTrue(searchApi.isIndexEmpty(), "SearchApi has no index") },
            { -> Assertions.assertTrue(state.visitedFilesNumber <= completedTotalFilesNumber, "visited <= total(precalculated)") },
            { -> Assertions.assertTrue(state.indexedFilesNumber > 0L, "indexedFilesNumber > 0") },
            { -> Assertions.assertEquals(1.0, state.progress, "progress == 1.0") }, //FIXME this logic
        )
        //totalFilesNumber can be null or defined. Cannot know by progress
    }

    /*Cancel after indexing.
    * Code shouldn't throw any exception, it should be saved index for folder
    * visited files number, indexed files number and total should be equal
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
            { -> Assertions.assertTrue(searchApi.hasIndexAtFolder(folder), "SearchApi has index at folder") },
            { -> Assertions.assertEquals(totalFilesNumber, state.visitedFilesNumber, "visited == total") },
            { -> Assertions.assertEquals(totalFilesNumber, state.indexedFilesNumber, "indexed == total") },
            { -> Assertions.assertEquals(1.0, state.progress, "progress == 1.0") },
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun asyncCancelAtProgress(indexingState: IndexingState, cancelAtProgress: Double, checkProgressEveryMillis: Long) {
        GlobalScope.async {
            while (!indexingState.finished) {
                val progress = indexingState.progress
                if (progress >= cancelAtProgress) {
                    indexingState.cancel()
                    break
                }
                delay(checkProgressEveryMillis)
            }
        }
    }

    companion object{
        private val inMiddleProgressList = listOf(
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9
        )
        @JvmStatic
        fun searchApiProvider(): Stream<Arguments> {
            return inMiddleProgressList
                .map{Arguments.of(it)}
                .stream()
        }
    }
}