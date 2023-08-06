package searchApi.indexing.features

import api.IndexingState
import api.emptyIndex
import api.tools.syncPerformIndex
import impl.trigram.TrigramSearchApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import searchApi.common.commonSetup
import java.nio.file.Path

/*TODO make tests
* - no progress increasing shortly after cancel
 */
/* Checks correctness of cancel of indexing in SearchApi
* */
class CancelTest {
    /*source code of intellij idea*/
    private val commonPath: Path = commonSetup.srcFolder

    /*using not by interface, because we use methods exactly from TrigramSearchApi*/
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /*Cancel during indexing.
    * Code shouldn't throw any exception, it should be saved no index
    * */
    @Test
    fun curProjectIndexingAndCancelDuringIndexTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val indexingState: IndexingState = searchApi.createIndexAtFolder(folder)
        asyncCancelAtProgress(
            indexingState = indexingState,
            cancelAtProgress = 0.2,
            checkProgressEveryMillis = 5
        )
        indexingState.result.get()!!
        Assertions.assertTrue(searchApi.emptyIndex())
    }

    /*Cancel after indexing.
    * Code shouldn't throw any exception, it should be saved index for folder
    * */
    @Test
    fun curProjectIndexingAndCancelAfterIndexTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val indexingState: IndexingState = searchApi.syncPerformIndex(folder)
        indexingState.cancel()
        Assertions.assertTrue(searchApi.hasIndexAtFolder(folder))
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun asyncCancelAtProgress(indexingState: IndexingState, cancelAtProgress: Double, checkProgressEveryMillis: Long) {
        GlobalScope.async {
            while (!indexingState.finished) {
                delay(checkProgressEveryMillis)
                val progress = indexingState.progress
                if (progress >= cancelAtProgress) {
                    indexingState.cancel()
                    break
                }
            }
        }
    }
}