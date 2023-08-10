package searchApi.indexing.features

import api.IndexingState
import api.IndexingStateSnapshot
import api.toSnapshot
import impl.trigram.TrigramSearchApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import searchApi.common.commonSetup
import searchApi.common.compareSets
import utils.paired
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

//TODO make more tests
//TODO make tests decomposition

/* Checks correctness of progress, visitedPathsBuffer, indexedPathsBuffer,
* totalFilesNumber, visitedFilesNumber, indexedFilesNumber in indexing for SearchApi
*
* */
class ProgressTest {

    /*source code of intellij idea* */
    private val commonPath: Path = commonSetup.srcFolder

    /*using not by interface, because we use methods exactly from TrigramSearchApi* */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /*Comparing snapshots for indexing state at different progresses
    * */
    @Test
    fun curProjectIndexingSequencialSnapshotsTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        val indexingStateSnapshotMap: Map<Double, IndexingStateSnapshot> =
            getSnapshotsAtProgresses(
                indexingState = state,
                progressStep = 0.1,
                checkProgressEveryMillis = 1
            )
        val resultPaths: List<Path> = state.result.get()!!
        val finishedSnapshot = state.toSnapshot()

        val snapshotListsByProgress: List<Pair<Double, IndexingStateSnapshot>> =
            indexingStateSnapshotMap.toSortedMap().toList()
        val snapshotList: List<IndexingStateSnapshot> = snapshotListsByProgress.map { it.second }
        val snapshotPairs: List<Pair<IndexingStateSnapshot, IndexingStateSnapshot>> = snapshotList.paired()

        val mapChecks = getChecksByMap(indexingStateSnapshotMap)
        val pairChecks = getPairChecks(snapshotPairs)
        val singleChecks = getSingleChecks(snapshotList)
        val accumulatingChecks = getAccumulatingChecks(snapshotList + finishedSnapshot, resultPaths)

        val allChecks: List<() -> Unit> = mapChecks + pairChecks + singleChecks + accumulatingChecks

        assertAll("progress checks", *allChecks.toTypedArray())
    }

    /*General checks by map of snapshots by progress
    * */
    private fun getChecksByMap(indexingStateSnapshotMap: Map<Double, IndexingStateSnapshot>): List<() -> Unit> =
        buildList<() -> Unit> {
            add { -> Assertions.assertTrue(indexingStateSnapshotMap.isNotEmpty(), "received at least 1 snapshots") }
            add { ->
                Assertions.assertTrue(
                    indexingStateSnapshotMap.containsKey(1.0),
                    "map should have snapshot at 1.0 progress"
                )
            }
        }

    /*Creates checks for all snapshots together.
    * Sum of all buffers for visiting and indexing should be the same as result paths
    * */
    private fun getAccumulatingChecks(
        snapshotList: List<IndexingStateSnapshot>,
        resultPaths: List<Path>
    ): List<() -> Unit> {
        val aggregatedVisitedPaths =
            snapshotList.fold(emptyList<Path>()) { curList, snapshot -> curList + snapshot.visitedPathsBuffer }
        val aggregatedIndexedPaths =
            snapshotList.fold(emptyList<Path>()) { curList, snapshot -> curList + snapshot.indexedPathsBuffer }
        val aggregatedVisitedPathsSet = aggregatedVisitedPaths.toSet()
        val aggregatedIndexedPathsSet = aggregatedIndexedPaths.toSet()
        val resultPathsSet = resultPaths.toSet()

        return compareSets(resultPathsSet, aggregatedVisitedPathsSet, "resultPaths", "visitedFilePaths", "path") +
                compareSets(resultPathsSet, aggregatedIndexedPathsSet, "resultPaths", "aggregatedIndexedPaths", "path")
    }

    /*Creates independent checks for snapshots for different components
     * */
    private fun getSingleChecks(snapshotList: List<IndexingStateSnapshot>): List<() -> Unit> =
        buildList<() -> Unit> {
            for (snapshot in snapshotList) {
                val total = snapshot.totalFilesNumber
                val finished = snapshot.finished
                val progress = snapshot.progress
                val indexedFilesNumber = snapshot.indexedFilesNumber
                val visitedFilesNumber = snapshot.visitedFilesNumber

                add { -> Assertions.assertTrue(progress >= 0.0, "progress >= 0.0") }
                add { -> Assertions.assertTrue(progress <= 1.0, "progress <= 1.0") }
                add { ->
                    Assertions.assertTrue(
                        visitedFilesNumber >= indexedFilesNumber,
                        "visitedFilesNumber >= indexedFilesNumber"
                    )
                }
                if (total != null) {
                    add { ->
                        Assertions.assertEquals(
                            total,
                            visitedFilesNumber,
                            " total == visitedFilesNumber, if total!=null"
                        )
                    }
                }
                if (finished) {
                    add { ->
                        Assertions.assertEquals(
                            total,
                            indexedFilesNumber,
                            " total == indexedFilesNumber, if finished"
                        )
                    }
                    add { -> Assertions.assertEquals(1.0, progress, " progress == 1.0, if finished") }
                }
                if (visitedFilesNumber == 0L) {
                    add { ->
                        Assertions.assertEquals(
                            0.0,
                            progress,
                            " progress == 0.0, if there is no indexed files yet"
                        )
                    }
                }
                if (total == null) {
                    add { ->
                        Assertions.assertEquals(
                            0.0,
                            progress,
                            " progress == 0.0, if total files number is not defined yet"
                        )
                    }
                }
                if (progress > 0.0) {
                    add { -> Assertions.assertNotNull(total, "total != null, if progress > 0.0") }
                    add { ->
                        Assertions.assertEquals(
                            total,
                            visitedFilesNumber,
                            " total == visitedFilesNumber, if progress > 0.0"
                        )
                    }
                }
            }
        }

    /*Creates checks for pairs of snapshots, which were created one after another
    * */
    private fun getPairChecks(snapshotPairs: List<Pair<IndexingStateSnapshot, IndexingStateSnapshot>>): List<() -> Unit> =
        buildList<() -> Unit> {
            for ((snapshot1, snapshot2) in snapshotPairs) {
                add { ->
                    Assertions.assertTrue(
                        snapshot1.progress < snapshot2.progress,
                        "snapshot1.progress < snapshot2.progress"
                    )
                }
                add { ->
                    Assertions.assertTrue(
                        snapshot1.visitedFilesNumber <= snapshot2.visitedFilesNumber,
                        "snapshot1.visitedFilesNumber < snapshot2.visitedFilesNumber"
                    )
                }
                add { ->
                    Assertions.assertTrue(
                        snapshot1.indexedFilesNumber <= snapshot2.indexedFilesNumber,
                        "snapshot1.indexedFilesNumber < snapshot2.indexedFilesNumber"
                    )
                }
            }
        }

    /*Takes state snapshots at different time of indexing.
    * Starts looking for moments between 'curLookingProgress'  and 'curLookingProgress' + progressStep.
    * Starts always from 0.0.
    * If it finds state between these values of progress, it takes snapshot and saves to map by progress value
    * It returns map of snapshots taken at progress save as a key
    * Additionally in the end it saves snapshot at 1.0, it wasn't saved before.
    * */
    @OptIn(DelicateCoroutinesApi::class)
    fun getSnapshotsAtProgresses(
        indexingState: IndexingState,
        progressStep: Double,
        checkProgressEveryMillis: Long
    ): Map<Double, IndexingStateSnapshot> {
        val indexingStateSnapshotMap = ConcurrentHashMap<Double, IndexingStateSnapshot>()
        GlobalScope.async {
            var curLookingProgress = 0.0
            while (!indexingState.finished) {
                val progress = indexingState.progress
                if (curLookingProgress <= progress && progress < curLookingProgress + progressStep) {
                    indexingStateSnapshotMap[curLookingProgress] = indexingState.toSnapshot()
                    curLookingProgress += progressStep
                } else if (progress > curLookingProgress + progressStep) {
                    //progress went too far, we need to apply step already
                    curLookingProgress += progressStep
                }
                delay(checkProgressEveryMillis)
            }
            if (!indexingStateSnapshotMap.containsKey(1.0)) {
                indexingStateSnapshotMap[1.0] = indexingState.toSnapshot()
            }
        }
        return indexingStateSnapshotMap
    }
}