package searchApi.searching.features.concurrency

import api.tools.searchapi.syncPerformIndex
import impl.trigram.TrigramSearchApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import java.util.stream.Stream

/**
 * Checks correctness of concurrent situations of searching in SearchApi
 * */
class ConcurrentTest {
    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi.
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Checking double searching of 2 folders with positive scenarios.
     * Before searching index is created for both situations.
     * For each of these scenarios should not be thrown any exception and after all,
     * it should be both not empty token matches as results.
     *
     * Scenarios have 4 options:
     *  - folder configuration:
     *      - same folder,
     *      - subfolder,
     *      - parent folder,
     *      - different folder
     *  - token configuration:
     *      - same token,
     *      - different tokens,
     *   - number of instances of SearchApi
     *      - single, one for both folders
     *      - dual, each folder has own searchApi
     *   - timing of calculating index
     *      - concurrent - asynchronous
     *      - sequential - synchronous
     * */
    @ParameterizedTest(name = "positiveCombinationTest{0}")
    @MethodSource("positiveConcurrencyTestCaseProvider")
    fun positiveCombinationTest(testCase: ConcurrencyTestCase) {
        //choosing folders
        val folder1 = testCase.folderCombination.folder1
        val folder2 = testCase.folderCombination.folder2
        //choosing tokens
        val token1 = testCase.tokenCombination.token1
        val token2 = testCase.tokenCombination.token2
        //preparing 1 or 2 instances of SearchApi
        val searchApi1 = searchApiGenerator()
        val searchApi2 = if (testCase.numberOfInstances == NumberOfInstances.DUAL) searchApiGenerator() else searchApi1
        //preparing indices before main logic
        searchApi1.syncPerformIndex(folder1)
        searchApi2.syncPerformIndex(folder2)
        //main searching. Depends on timing synchronous or asynchronous
        val searchingState1 = testCase.timing.makeSearch(searchApi1, folder1, token1)
        val searchingState2 = testCase.timing.makeSearch(searchApi2, folder2, token2)
        //waiting finishing search for both states
        val tokenMatches1 = searchingState1.result.get()
        val tokenMatches2 = searchingState2.result.get()
        assertAll({ Assertions.assertTrue(tokenMatches1.isNotEmpty(), "tokenMatches1 is not empty") },
            { Assertions.assertTrue(tokenMatches2.isNotEmpty(), "tokenMatches2 is not empty") })
    }

    /**
     * Checks searching 10 tokens in one folder simultaneously, all should receive results.
     * Before searching index is created for folder
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun tenTokensTest() {
        val folder = commonSetup.srcFolder
        val tokens = listOf("class", "fun", "index", "search", "token", "folder", "sync", "suspend", "return", "import")
        val searchApi = searchApiGenerator()
        searchApi.syncPerformIndex(folder)
        val tokenMatchesList = runBlocking {
            val stateDeferreds = tokens.map { async { searchApi.searchString(folder, it) } }
            stateDeferreds.forEach { it.join() }
            val states = stateDeferreds.map { it.getCompleted() }
            states.map { it.result.get() }
        }
        val checks = tokens.zip(tokenMatchesList).map { (token, tokenMatches) ->
            { Assertions.assertTrue(tokenMatches.isNotEmpty(), "tokenMatches for token \"$token\" is not empty") }
        }
        assertAll(
            { Assertions.assertEquals(10, tokens.size, "There are 10 tokens") },
            { Assertions.assertEquals(10, tokenMatchesList.size, "There are 10 elements in tokenMatchesList") },
            *checks.toTypedArray()
        )
    }

    companion object {
        /**
         * Arguments with all positive ConcurrencyTestCases
         * */
        @JvmStatic
        fun positiveConcurrencyTestCaseProvider(): Stream<Arguments> {
            return positiveConcurrencyTestCases.stream().map { Arguments.of(it) }
        }

        /**
         * All combinations for ConcurrencyTestCase
         * */
        private val positiveConcurrencyTestCases: List<ConcurrencyTestCase> = buildList {
            for (timing in Timing.entries) {
                for (tokenCombination in TokenCombination.entries) {
                    for (numberOfInstances in NumberOfInstances.entries) {
                        for (folderCombination in FolderCombination.entries) {
                            add(ConcurrencyTestCase(timing, tokenCombination, numberOfInstances, folderCombination))
                        }
                    }
                }
            }
        }
    }
}