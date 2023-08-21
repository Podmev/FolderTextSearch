package api.tools.state

import api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Takes state snapshots at different time of indexing.
 * Starts looking for moments between 'curLookingProgress'  and 'curLookingProgress' + progressStep.
 * Starts always from 0.0.
 * If it finds state between these values of progress, it takes snapshot and saves to map by progress value
 * It returns map of snapshots taken at progress save as a key
 * Additionally in the end it saves snapshot at 1.0, it wasn't saved before.
 * */
fun getIndexingSnapshotsAtProgresses(
    indexingState: IndexingState, progressStep: Double, checkProgressEveryMillis: Long
): Map<Double, IndexingStateSnapshot> = getSnapshotsAtProgresses(
    state = indexingState,
    getSnapshot = fun(state: IndexingState): IndexingStateSnapshot = state.toSnapshot(),
    progressStep = progressStep,
    checkProgressEveryMillis = checkProgressEveryMillis
)

/**
 * Takes state snapshots at different time of searching.
 * Starts looking for moments between 'curLookingProgress'  and 'curLookingProgress' + progressStep.
 * Starts always from 0.0.
 * If it finds state between these values of progress, it takes snapshot and saves to map by progress value
 * It returns map of snapshots taken at progress save as a key
 * Additionally in the end it saves snapshot at 1.0, it wasn't saved before.
 * */
fun getSearchingSnapshotsAtProgresses(
    searchingState: SearchingState, progressStep: Double, checkProgressEveryMillis: Long
): Map<Double, SearchingStateSnapshot> = getSnapshotsAtProgresses(
    state = searchingState,
    getSnapshot = fun(state: SearchingState): SearchingStateSnapshot = state.toSnapshot(),
    progressStep = progressStep,
    checkProgressEveryMillis = checkProgressEveryMillis
)

/**
 * Takes state snapshots at different time of ProgressableState.
 * Starts looking for moments between 'curLookingProgress'  and 'curLookingProgress' + progressStep.
 * Starts always from 0.0.
 * If it finds state between these values of progress, it takes snapshot and saves to map by progress value
 * It returns map of snapshots taken at progress save as a key
 * Additionally in the end it saves snapshot at 1.0, it wasn't saved before.
 * */
fun <State : ProgressableState, Snapshot> getSnapshotsAtProgresses(
    state: State, getSnapshot: (State) -> Snapshot, progressStep: Double, checkProgressEveryMillis: Long
): Map<Double, Snapshot> = runBlocking {
    val snapshotMap = HashMap<Double, Snapshot>()
    // as runBlocking executes in the current block, launch + join is effectively unnecessary
    var curLookingProgress = 0.0
    while (!state.finished) {
        val progress = state.progress
        if (curLookingProgress <= progress && progress < curLookingProgress + progressStep) {
            snapshotMap[curLookingProgress] = getSnapshot(state)
            curLookingProgress += progressStep
        } else if (progress > curLookingProgress + progressStep) {
            //progress went too far, we need to apply step already
            curLookingProgress += progressStep
        }
        delay(checkProgressEveryMillis)
    }
    if (!snapshotMap.containsKey(1.0) && state.progress == 1.0) {
        snapshotMap[1.0] = getSnapshot(state)
    }
    snapshotMap
}