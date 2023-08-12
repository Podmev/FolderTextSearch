package searchApi.searching.features

import api.*
import api.tools.searchapi.syncPerformIndex
import api.tools.state.getSearchingSnapshotAtProgress
import api.tools.state.getSearchingSnapshotsAtProgresses
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.TwoObjectsComparator
import searchApi.common.commonSetup
import searchApi.common.compareSets
import utils.paired
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Checks correctness of progress, visitedPathsBuffer, tokenMatchesBuffer,
 * totalFilesNumber, visitedFilesNumber, visitedFilesByteSize, parsedFilesByteSize, totalFilesByteSize in searching for SearchApi
 * */
class ProgressTest {

    /**
     * Source code of current project
     * */
    private val commonPath: Path = commonSetup.srcFolder

    /**
     * Typical common token to search.
     * */
    private val commonToken: String = "class"

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Checking snapshots for searching state at different progresses in general
     * */
    @Test
    fun generalCompareSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val snapshotMap: Map<Double, SearchingStateSnapshot> =
            getSearchingSnapshotsAtProgresses(searchingState = state, progressStep = 0.1, checkProgressEveryMillis = 1)
        state.result.get()

        assertAll("general checks",
            { assertTrue(snapshotMap.isNotEmpty(), "received at least 1 snapshots") },
            { assertTrue(snapshotMap.containsKey(1.0), "map should have snapshot at 1.0 progress") })
    }

    /**
     * Checking snapshot at progress for searching state at different progresses.
     * Checks about start or 0% progress.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("progressProvider")
    fun snapshotStartChecksAtProgressTest(checkAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val snapshot = getSearchingSnapshotAtProgress(
            searchingState = state, progress = checkAtProgress, checkProgressEveryMillis = 1
        )
        state.result.get()
        val totalFileNumber = snapshot.totalFilesNumber
        val totalFileByteSize = snapshot.totalFilesByteSize
        val progress = snapshot.progress
        val visitedFilesNumber = snapshot.visitedFilesNumber
        val visitedFilesByteSize = snapshot.visitedFilesByteSize
        val checks = buildList {
            add { assertTrue(progress >= 0.0, "progress >= 0.0") }
            if (visitedFilesNumber == 0L) {
                add { assertEquals(0.0, progress, "progress==0.0, if there is no visited files yet") }
            }
            if (visitedFilesByteSize == 0L) {
                val condMsg = ", if there is no saved visited files byte sizes"
                add { assertEquals(0.0, progress, "progress==0.0$condMsg") }
            }
            if (totalFileNumber == null) {
                val condMsg = ", if total files number is not defined yet"
                add { assertEquals(0.0, progress, "progress==0.0$condMsg") }
            }
            if (totalFileByteSize == null) {
                val condMsg = ", if total files byte size is not defined yet"
                add { assertEquals(0.0, progress, "progress==0.0$condMsg") }
            }
        }
        assertAll("progress checks", checks)
    }

    /**
     * Checking snapshot at progress for searching state at different progresses
     * Checks about middle state of in general
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("progressProvider")
    fun snapshotMiddleChecksAtProgressTest(checkAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val snapshot = getSearchingSnapshotAtProgress(
            searchingState = state, progress = checkAtProgress, checkProgressEveryMillis = 1
        )
        state.result.get()
        val totalFileNumber = snapshot.totalFilesNumber
        val totalFileByteSize = snapshot.totalFilesByteSize
        val progress = snapshot.progress
        val visitedFilesNumber = snapshot.visitedFilesNumber
        val visitedFilesByteSize = snapshot.visitedFilesByteSize
        val parsedFilesByteSize = snapshot.parsedFilesByteSize
        val checks = buildList {
            add { assertTrue(visitedFilesByteSize >= parsedFilesByteSize, "visitedFilesByteSize>=parsedFilesByteSize") }
            if (totalFileNumber != null) {
                val condMsg = "total==visitedFilesNumber, if total!=null"
                add { assertEquals(totalFileNumber, visitedFilesNumber, "total==visitedFilesNumber$condMsg") }
            }
            if (progress > 0.0) {
                val condMsg = ", if progress > 0.0"
                add { assertNotNull(totalFileNumber, "total!=null$condMsg") }
                add { assertNotNull(totalFileByteSize, "total!=null$condMsg") }
                add { assertEquals(totalFileNumber, visitedFilesNumber, "total==visitedFilesNumber$condMsg") }
                add { assertEquals(totalFileByteSize, visitedFilesByteSize, "total==visitedFilesByteSize$condMsg") }
            }
        }
        assertAll("progress checks", checks)
    }

    /**
     * Checking snapshot at progress for searching state at different progresses.
     * Checks about start or 0% progress.
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("progressProvider")
    fun snapshotFinishChecksAtProgressTest(checkAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val snapshot = getSearchingSnapshotAtProgress(
            searchingState = state, progress = checkAtProgress, checkProgressEveryMillis = 1
        )
        state.result.get()
        val totalFileByteSize = snapshot.totalFilesByteSize
        val finished = snapshot.status == ProgressableStatus.FINISHED
        val progress = snapshot.progress
        val parsedFilesByteSize = snapshot.parsedFilesByteSize
        val checks = buildList {
            add { assertTrue(progress <= 1.0, "progress <= 1.0") }
            if (finished) {
                val condMsg = ", if finished"
                add { assertEquals(totalFileByteSize, parsedFilesByteSize, "total==searchedFilesNumber$condMsg") }
                add { assertEquals(1.0, progress, "progress==1.0, if finished") }
            }
        }
        assertAll("progress checks", checks)
    }

    /**
     * Comparing sequential snapshots by pairs for searching state at different progresses
     * */
    @Test
    fun byPairsCompareSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath

        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val searchingStateSnapshotMap: Map<Double, SearchingStateSnapshot> = getSearchingSnapshotsAtProgresses(
            searchingState = state, progressStep = 0.1, checkProgressEveryMillis = 1
        )
        state.result.get()
        val snapshotListsByProgress: List<Pair<Double, SearchingStateSnapshot>> =
            searchingStateSnapshotMap.toSortedMap().toList()
        val snapshotList: List<SearchingStateSnapshot> = snapshotListsByProgress.map { it.second }
        val snapshotPairs: List<Pair<SearchingStateSnapshot, SearchingStateSnapshot>> = snapshotList.paired()

        val allChecks: List<() -> Unit> = buildList {
            for ((snapshot1, snapshot2) in snapshotPairs) {
                val comparator: TwoObjectsComparator<SearchingStateSnapshot> =
                    TwoObjectsComparator(snapshot1, snapshot2, "snapshot1", "snapshot2")
                add { comparator.assert("<", SearchingStateSnapshot::progress, "progress") }
                add { comparator.assert("<", SearchingStateSnapshot::visitedFilesNumber, "visitedFilesNumber") }
                add { comparator.assert("<", SearchingStateSnapshot::visitedFilesByteSize, "visitedFilesByteSize") }
                add { comparator.assert("<", SearchingStateSnapshot::parsedFilesByteSize, "parsedFilesByteSize") }
                add { comparator.assert("<", SearchingStateSnapshot::tokenMatchesNumber, "tokenMatchesNumber") }
                add { comparator.assert("<", SearchingStateSnapshot::totalTime, "totalTime") }
            }
        }
        assertAll("progress checks", *allChecks.toTypedArray())
    }

    /**
     * Checking snapshots in total for searching state at different progresses
     * Checking that buffers from all snapshots
     * including the one in the end together give visitedFilesNumber and tokenMatchesNumber at finalSnapshot
     * */
    @Test
    fun totalNumberChecksSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val searchingStateSnapshotMap: Map<Double, SearchingStateSnapshot> = getSearchingSnapshotsAtProgresses(
            searchingState = state, progressStep = 0.1, checkProgressEveryMillis = 1
        )
        state.result.get()
        val finishedSnapshot = state.toSnapshot()
        val snapshotListsByProgress: List<Pair<Double, SearchingStateSnapshot>> =
            searchingStateSnapshotMap.toSortedMap().toList()
        val snapshotList: List<SearchingStateSnapshot> = snapshotListsByProgress.map { it.second } + finishedSnapshot
        val aggregatedVisitedPaths =
            snapshotList.fold(emptyList<Path>()) { curList, snapshot -> curList + snapshot.visitedPathsBuffer }
        val aggregatedTokenMatches =
            snapshotList.fold(emptyList<TokenMatch>()) { curList, snapshot -> curList + snapshot.tokenMatchesBuffer }
        val aggregatedVisitedPathsSet = aggregatedVisitedPaths.toSet()
        val aggregatedTokenMatchesSet = aggregatedTokenMatches.toSet()

        val totalBufferSizesChecks: List<() -> Unit> = buildList {
            add {
                assertEquals(
                    finishedSnapshot.visitedFilesNumber,
                    aggregatedVisitedPathsSet.size.toLong(),
                    "Total visited files buffers sizes = total visited files number size"
                )
            }
            add {
                assertEquals(
                    finishedSnapshot.tokenMatchesNumber,
                    aggregatedTokenMatchesSet.size.toLong(),
                    "Total token matches buffers sizes = total number of token matches"
                )
            }
        }
        assertAll("progress checks", totalBufferSizesChecks)
    }

    /**
     * Checking snapshots in total for searching state at different progresses
     * Checking that buffers from all snapshots including the one in the end together
     * give all token matches given as result
     * */
    @Test
    fun totalSetChecksSnapshotsAtDifferentProgressesTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, commonToken)
        val searchingStateSnapshotMap: Map<Double, SearchingStateSnapshot> = getSearchingSnapshotsAtProgresses(
            searchingState = state, progressStep = 0.1, checkProgressEveryMillis = 1
        )
        val resultTokenMatches = state.result.get()
        val finishedSnapshot = state.toSnapshot()
        val snapshotListsByProgress: List<Pair<Double, SearchingStateSnapshot>> =
            searchingStateSnapshotMap.toSortedMap().toList()
        val snapshotList: List<SearchingStateSnapshot> = snapshotListsByProgress.map { it.second } + finishedSnapshot
        val aggregatedTokenMatches =
            snapshotList.fold(emptyList<TokenMatch>()) { curList, snapshot -> curList + snapshot.tokenMatchesBuffer }
        val aggregatedTokenMatchesSet = aggregatedTokenMatches.toSet()
        val resultTokenMatchesSet = resultTokenMatches.toSet()

        val tokenMatchesSetChecks = compareSets(
            resultTokenMatchesSet,
            aggregatedTokenMatchesSet,
            "resultTokenMatches",
            "aggregatedTokenMatches",
            "tokenMatch"
        )
        assertAll("progress checks", tokenMatchesSetChecks)
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
