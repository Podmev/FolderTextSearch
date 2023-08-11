package searchApi.searching.features

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
import kotlin.test.Ignore

//TODO make tests
/**
 * Checks correctness of cancel of searching in SearchApi
 * */
@Suppress("DeferredResultUnused")
@Ignore
class CancelTest {
    /**
     * Source code of current project
     * */
    private val commonPath: Path = commonSetup.srcFolder

    /**
     * typical common token to search
     * */
    private val commonToken: String = "class"

    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Cancel at the start searching.
     * Code shouldn't throw any exception, it should be saved no search.
     * */
    @Test
    fun curProjectSearchingAndCancelAtStartSearchingTest() {
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
            checkProgressEveryMillis = 1
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
            { Assertions.assertEquals(1.0, state.progress, "progress == 1.0") },
        )
    }

    /**
     * Cancel during searching at progress cancelAtProgress.
     * Code shouldn't throw any exception, it should give any results
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("searchApiProvider")
    fun curProjectSearchingAndCancelDuringSearchTest(cancelAtProgress: Double) {
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
            checkProgressEveryMillis = 1
        )
        val tokenMatches = state.result.get()
        Assertions.assertAll(
            { Assertions.assertTrue(tokenMatches.isEmpty(), "No token matches on cancel") },
            { Assertions.assertFalse(previousTokenMatches.isEmpty(), "previousTokenMatches are not empty") },
            {
                Assertions.assertTrue(
                    state.visitedFilesByteSize <= completedTotalFilesByteSize,
                    "visited <= total(precalculated) in bytes"
                )
            },
            {
                Assertions.assertTrue(
                    state.parsedFilesByteSize <= completedTotalFilesByteSize,
                    "parsed <= total(precalculated) in bytes"
                )
            },
            { Assertions.assertTrue(state.parsedFilesByteSize > 0L, "parsedFilesByteSize > 0") },
            { Assertions.assertEquals(1.0, state.progress, "progress == 1.0") }, //FIXME this logic
        )
        //totalFilesNumber can be null or defined. Cannot know by progress
    }

    /**
     * Cancel after searching.
     * Code shouldn't throw any exception, it should find tokens in folder files.
     * Visited files byte size, parsed files byte size, total files byte size should be equal
     * */
    @Test
    fun curProjectSearchingAndCancelAfterSearchingTest() {
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
        )
    }

    companion object {
        private val inMiddleProgressList = listOf(
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9
        )

        @JvmStatic
        fun searchApiProvider(): Stream<Arguments> {
            return inMiddleProgressList
                .map { Arguments.of(it) }
                .stream()
        }
    }
}