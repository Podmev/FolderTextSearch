package searchApi.indexing.features

import api.IndexingState
import api.IndexingStateSnapshot
import api.toSnapshot
import api.tools.state.getIndexingSnapshotAtProgress
import api.tools.state.getIndexingSnapshotsAtProgresses
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import searchApi.common.compareSets
import utils.paired
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Checks correctness of progress, visitedPathsBuffer, indexedPathsBuffer,
 * totalFilesNumber, visitedFilesNumber, indexedFilesNumber in indexing for SearchApi
 * */
class ProgressTest {

    /**
     * Source code of current project
     * */
    private val commonPath: Path = commonSetup.srcFolder

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Checking snapshots for indexing state at different progresses in general
     * */
    @Test
    fun generalCompareSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        val indexingStateSnapshotMap: Map<Double, IndexingStateSnapshot> =
            getIndexingSnapshotsAtProgresses(indexingState = state, progressStep = 0.1, checkProgressEveryMillis = 1)
        state.result.get()

        assertAll("general checks",
            { assertTrue(indexingStateSnapshotMap.isNotEmpty(), "received at least 1 snapshots") },
            {
                assertTrue(
                    indexingStateSnapshotMap.containsKey(1.0), "map should have snapshot at 1.0 progress"
                )
            })
    }

    /**
     * Checking snapshot at progress for indexing state at different progresses
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("progressProvider")
    fun snapshotChecksAtProgressTest(checkAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        val snapshot = getIndexingSnapshotAtProgress(
            indexingState = state, progress = checkAtProgress, checkProgressEveryMillis = 1
        )
        state.result.get()
        val total = snapshot.totalFilesNumber
        val finished = snapshot.finished
        val progress = snapshot.progress
        val indexedFilesNumber = snapshot.indexedFilesNumber
        val visitedFilesNumber = snapshot.visitedFilesNumber
        val checks = buildList {
            add { assertTrue(progress >= 0.0, "progress >= 0.0") }
            add { assertTrue(progress <= 1.0, "progress <= 1.0") }
            add { assertTrue(visitedFilesNumber >= indexedFilesNumber, "visitedFilesNumber >= indexedFilesNumber") }
            if (total != null) {
                add { assertEquals(total, visitedFilesNumber, "total == visitedFilesNumber, if total!=null") }
            }
            if (finished) {
                add { assertEquals(total, indexedFilesNumber, "total == indexedFilesNumber, if finished") }
                add { assertEquals(1.0, progress, "progress == 1.0, if finished") }
            }
            if (visitedFilesNumber == 0L) {
                add { assertEquals(0.0, progress, "progress == 0.0, if there is no indexed files yet") }
            }
            if (total == null) {
                add { assertEquals(0.0, progress, "progress == 0.0, if total files number is not defined yet") }
            }
            if (progress > 0.0) {
                add { assertNotNull(total, "total != null, if progress > 0.0") }
                add { assertEquals(total, visitedFilesNumber, "total == visitedFilesNumber, if progress > 0.0") }
            }
        }
        assertAll("progress checks", checks)
    }

    /**
     * Comparing sequential snapshots by pairs for indexing state at different progresses
     * */
    @Test
    fun byPairsCompareSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        val indexingStateSnapshotMap: Map<Double, IndexingStateSnapshot> = getIndexingSnapshotsAtProgresses(
            indexingState = state, progressStep = 0.1, checkProgressEveryMillis = 1
        )
        state.result.get()
        val snapshotListsByProgress: List<Pair<Double, IndexingStateSnapshot>> =
            indexingStateSnapshotMap.toSortedMap().toList()
        val snapshotList: List<IndexingStateSnapshot> = snapshotListsByProgress.map { it.second }
        val snapshotPairs: List<Pair<IndexingStateSnapshot, IndexingStateSnapshot>> = snapshotList.paired()

        val allChecks: List<() -> Unit> = buildList {
            for ((snapshot1, snapshot2) in snapshotPairs) {
                val s1 = "snapshot1"
                val s2 = "snapshot2"
                val progressCondition = snapshot1.progress < snapshot2.progress
                val visitedFilesNumberCondition = snapshot1.visitedFilesNumber <= snapshot2.visitedFilesNumber
                val indexedFilesNumberCondition = snapshot1.indexedFilesNumber <= snapshot2.indexedFilesNumber
                add { assertTrue(progressCondition, "$s1.progress < $s2.progress") }
                add { assertTrue(visitedFilesNumberCondition, "$s1.visitedFilesNumber < $s2.visitedFilesNumber") }
                add { assertTrue(indexedFilesNumberCondition, "$s1.indexedFilesNumber < $s2.indexedFilesNumber") }
            }
        }
        assertAll("progress checks", *allChecks.toTypedArray())
    }

    /**
     * Checking snapshots in total for indexing state at different progresses
     * Checking that buffers from all snapshots, including in the end together give all paths given as result
     * */
    @Test
    fun totalChecksSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val state: IndexingState = searchApi.createIndexAtFolder(folder)
        val indexingStateSnapshotMap: Map<Double, IndexingStateSnapshot> = getIndexingSnapshotsAtProgresses(
            indexingState = state, progressStep = 0.1, checkProgressEveryMillis = 1
        )
        state.result.get()
        val resultPaths: List<Path> = state.result.get()!!
        val finishedSnapshot = state.toSnapshot()

        val snapshotListsByProgress: List<Pair<Double, IndexingStateSnapshot>> =
            indexingStateSnapshotMap.toSortedMap().toList()
        val snapshotList: List<IndexingStateSnapshot> = snapshotListsByProgress.map { it.second } + finishedSnapshot

        val aggregatedVisitedPaths =
            snapshotList.fold(emptyList<Path>()) { curList, snapshot -> curList + snapshot.visitedPathsBuffer }
        val aggregatedIndexedPaths =
            snapshotList.fold(emptyList<Path>()) { curList, snapshot -> curList + snapshot.indexedPathsBuffer }

        val aggregatedVisitedPathsSet = aggregatedVisitedPaths.toSet()
        val aggregatedIndexedPathsSet = aggregatedIndexedPaths.toSet()
        val resultPathsSet = resultPaths.toSet()

        val visitedSetsChecks: List<() -> Unit> =
            compareSets(resultPathsSet, aggregatedVisitedPathsSet, "resultPaths", "visitedFilePaths", "path")
        val indexedSetsChecks =
            compareSets(resultPathsSet, aggregatedIndexedPathsSet, "resultPaths", "aggregatedIndexedPaths", "path")
        val allChecks: List<() -> Unit> = visitedSetsChecks + indexedSetsChecks

        assertAll("progress checks", allChecks)
    }

    companion object {
        /**
         * Values for progress, which are not 0.0 and 1.0. Used for parametrized test.
         * */
        private val progressList = listOf(
            0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0
        )

        /**
         * Provides arguments for tests: progress
         * */
        @JvmStatic
        fun progressProvider(): Stream<Arguments> {
            return progressList.map { Arguments.of(it) }.stream()
        }
    }
}