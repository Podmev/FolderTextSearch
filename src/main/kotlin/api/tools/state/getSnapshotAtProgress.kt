package api.tools.state

import api.*
import kotlinx.coroutines.*

/**
 * Gets indexing snapshot from indexingState at progress, works synchronous
 */
fun getIndexingSnapshotAtProgress(
    indexingState: IndexingState, progress: Double, checkProgressEveryMillis: Long
): IndexingStateSnapshot = getSnapshotAtProgress(
    indexingState,
    fun(state: IndexingState): IndexingStateSnapshot = state.toSnapshot(),
    progress,
    checkProgressEveryMillis
)

/**
 * Gets searching snapshot from searchingState at progress, works synchronous
 */
fun getSearchingSnapshotAtProgress(
    searchingState: SearchingState, progress: Double, checkProgressEveryMillis: Long
): SearchingStateSnapshot = getSnapshotAtProgress(
    searchingState,
    fun(state: SearchingState): SearchingStateSnapshot = state.toSnapshot(),
    progress,
    checkProgressEveryMillis
)

/**
 * Gets abstract snapshot from ProgressableState at progress, works synchronous
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <State : ProgressableState, Snapshot> getSnapshotAtProgress(
    state: State, getSnapshot: (State) -> Snapshot, progress: Double, checkProgressEveryMillis: Long
): Snapshot = runBlocking {
    val deferred: Deferred<Snapshot?> = async {
        while (!state.finished) {
            if (state.progress >= progress) {
                return@async getSnapshot(state)
            }
            delay(checkProgressEveryMillis)
        }
        if (state.progress >= progress) {
            return@async getSnapshot(state)
        }
        return@async null
    }
    deferred.join()
    deferred.getCompleted()!!
}