package api.tools.state

import api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Gets indexing snapshot from indexingState at progress, works synchronous
 */
fun getIndexingSnapshotAtProgress(
    indexingState: IndexingState, progress: Double, checkProgressEveryMillis: Long
): IndexingStateSnapshot = getSnapshotAtProgress(
    indexingState,
    progress,
    checkProgressEveryMillis
) { state: IndexingState -> state.toSnapshot() }

/**
 * Gets searching snapshot from searchingState at progress, works synchronous
 */
fun getSearchingSnapshotAtProgress(
    searchingState: SearchingState, progress: Double, checkProgressEveryMillis: Long
): SearchingStateSnapshot = getSnapshotAtProgress(
    searchingState,
    progress,
    checkProgressEveryMillis
) { state: SearchingState -> state.toSnapshot() }

/**
 * Gets abstract snapshot from ProgressableState at progress, works synchronous
 */
fun <State : ProgressableState, Snapshot> getSnapshotAtProgress(
    state: State, progress: Double, checkProgressEveryMillis: Long, getSnapshot: (State) -> Snapshot
): Snapshot = runBlocking {
    while (!state.finished && state.progress < progress) {
        delay(checkProgressEveryMillis)
    }
    return@runBlocking getSnapshot(state)
}