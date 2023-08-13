package searchApi.searching.features

import api.ProgressableStatus
import api.SearchingState
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
 * Checks correctness of cancel of searching in SearchApi.
 * */
@Suppress("DeferredResultUnused")
class CancelTest {
    /**
     * Source code of current project.
     * */
    private val commonPath: Path = commonSetup.srcFolder

    /**
     * Typical common token to search.
     * */
    private val commonToken: String = "class"

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi.
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Cancel at the start searching.
     * Code shouldn't throw any exception, it should be saved no search.
     * */
    @Test
    fun searchingAndCancelAtStartSearchingTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val token  = commonToken

        searchApi.syncPerformIndex(folder)
        val previouslyFinishedState = searchApi.searchString(folder, token)
        val previousTokenMatches = previouslyFinishedState.result.get()

        /*total should exist* */
        val completedTotalFilesNumber = previouslyFinishedState.totalFilesNumber!!

        val state: SearchingState = searchApi.searchString(folder, token)
        state.asyncCancelAtProgress(
            cancelAtProgress = 0.0,
            checkProgressEveryMillis = 0
        )
        val tokenMatches = state.result.get()
        Assertions.assertAll(
            { Assertions.assertFalse(previousTokenMatches.isEmpty(), "PreviousTokenMatches are not empty") },
            { Assertions.assertTrue(tokenMatches.isEmpty(), "No token matches on cancel") },
            {
                Assertions.assertTrue(
                    state.visitedFilesNumber < completedTotalFilesNumber,
                    "Parsed files number ${state.visitedFilesNumber} < total(precalculated) $completedTotalFilesNumber"
                )
            },
            { Assertions.assertEquals(0L, state.parsedFilesByteSize, "parsedFilesByteSize == 0") },
            { Assertions.assertEquals(null, state.totalFilesNumber, "totalFilesNumber == null") },
            { Assertions.assertEquals(0.0, state.progress, "progress == 0.0") },
            { Assertions.assertEquals(ProgressableStatus.CANCELLED, state.status, "status == CANCELLED") },
        )
    }

    /**
     * Cancel during searching at progress cancelAtProgress.
     * Code shouldn't throw any exception, it should give any results
     * */
    @ParameterizedTest(name = "searchingAndCancelDuringSearchTest{0}")
    @MethodSource("progressProvider")
    fun searchingAndCancelDuringSearchTest(cancelAtProgress: Double) {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val token = commonToken

        searchApi.syncPerformIndex(folder)
        val previouslyFinishedState = searchApi.searchString(folder, token)
        val previousTokenMatches = previouslyFinishedState.result.get()

        /*total should exist* */
        val completedTotalFilesByteSize = previouslyFinishedState.totalFilesByteSize!!

        val state: SearchingState = searchApi.searchString(folder, token)
        state.asyncCancelAtProgress(
            cancelAtProgress = cancelAtProgress,
            checkProgressEveryMillis = 0 //test is too small for any delay
        )
        val tokenMatches = state.result.get()
        val progress = state.progress
        Assertions.assertAll(
            { Assertions.assertTrue(tokenMatches.isEmpty(), "No token matches on cancel") },
            { Assertions.assertFalse(previousTokenMatches.isEmpty(), "previousTokenMatches are not empty") },
            {
                Assertions.assertEquals(
                    completedTotalFilesByteSize,
                    state.visitedFilesByteSize,
                    "visited = total(precalculated) in bytes"
                )
            },
            {
                Assertions.assertTrue(
                    state.parsedFilesByteSize <= completedTotalFilesByteSize,
                    "parsed <= total(precalculated) in bytes"
                )
            },
            { Assertions.assertTrue(state.parsedFilesByteSize > 0L, "parsedFilesByteSize > 0") },
            { Assertions.assertTrue(progress >= cancelAtProgress, "progress >= cancelAtProgress") },
            { Assertions.assertTrue(progress < 1.0, "progress < 1.0") },
            //In CI/CD doesn't work well, but locally works
            //{ Assertions.assertTrue(progress < cancelAtProgress + 0.15, "progress ($progress) < cancelAtProgress + 0.15") },
            { Assertions.assertEquals(ProgressableStatus.CANCELLED, state.status, "status == CANCELLED") },
            { Assertions.assertNotNull(state.totalFilesNumber, "totalFilesNumber is not null") },
            { Assertions.assertNotNull(state.totalFilesByteSize, "totalFilesByteSize is not null") },
        )
    }

    /**
     * Cancel after searching.
     * Code shouldn't throw any exception, it should find tokens in folder files.
     * Visited files byte size, parsed files byte size, total files byte size should be equal
     * */
    @Test
    fun searchingAndCancelAfterSearchingTest() {
        val searchApi = searchApiGenerator()
        val folder = commonPath
        val token = commonToken

        searchApi.syncPerformIndex(folder)
        val state: SearchingState = searchApi.searchString(folder, token)
        val tokenMatches = state.result.get()
        state.cancel()
        /*total should exist* */
        val totalFilesNumber = state.totalFilesNumber!!
        /*total should exist* */
        val totalFilesByteSize = state.totalFilesByteSize!!
        Assertions.assertAll(
            { Assertions.assertFalse(tokenMatches.isEmpty(), "Token matches are not empty") },
            { Assertions.assertEquals(totalFilesNumber, state.visitedFilesNumber,"visited == total files number") },
            { Assertions.assertEquals(totalFilesByteSize, state.visitedFilesByteSize, "visited == total in bytes") },
            { Assertions.assertEquals(totalFilesByteSize, state.parsedFilesByteSize, "parsed == total in bytes") },
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