package searchApi.indexing.features.concurrency

import api.exception.BusySearchException
import api.exception.NotDirSearchException
import api.tools.searchapi.syncPerformIndex
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import searchApi.common.commonSetup
import searchApi.common.compareSets
import searchApi.indexing.features.concurrency.FolderCombination.*
import searchApi.indexing.features.concurrency.IndexPresence.*
import searchApi.indexing.features.concurrency.NumberOfInstances.DUAL
import searchApi.indexing.features.concurrency.NumberOfInstances.SINGLE
import searchApi.indexing.features.concurrency.Timing.CONCURRENT
import searchApi.indexing.features.concurrency.Timing.SEQUENCIAL
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * Checks correctness of concurrent situations of indexing in SearchApi
 *
 * Both positiveCombinationTest and negativeCombinationTest together cover all combinations of ConcurrencyTestCase.
 * Check that they cover all combinations is checkIsCompleteConcurrencyTestCasesTest
 * */
class ConcurrentTest {
    /**
     * Using not by interface, because we use methods exactly from TrigramSearchApi.
     * */
    private val searchApiGenerator: () -> TrigramSearchApi = { TrigramSearchApi() }

    /**
     * Checking double indexing of 2 folders with positive scenarios
     * For each of these scenarios should not be thrown any exception and after all,
     * it should be both indices in SearchApi.
     *
     * Scenarios have 4 options:
     *  - folder configuration:
     *      - same folder,
     *      - subfolder,
     *      - parent folder,
     *      - different folder
     *  - index presence - before indexing should be the index for each folder or no:
     *      - no index,
     *      - only fist folder,
     *      - only second folder,
     *   - number of instances of SearchApi
     *      - single, one for both folders
     *      - dual, each folder has own searchApi
     *   - timing of calculating index
     *      - concurrent - asynchronous
     *      - sequential - synchronous
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("positiveConcurrencyTestCaseProvider")
    fun positiveCombinationTest(testCase: ConcurrencyTestCase) {
        //choosing folders
        val folder1 = testCase.folderCombination.folder1
        val folder2 = testCase.folderCombination.folder2
        //preparing 1 or 2 instances of SearchApi
        val searchApi1 = searchApiGenerator()
        val searchApi2 = if (testCase.numberOfInstances == DUAL) searchApiGenerator() else searchApi1
        //preparing indices before main logic - synchronous indexing if needed for each folder
        if (testCase.indexPresence.hasFolder1Index) searchApi1.syncPerformIndex(folder1)
        if (testCase.indexPresence.hasFolder2Index) searchApi2.syncPerformIndex(folder2)
        //main indexing. Depends on timing synchronous or asynchronous
        val indexingState1 = testCase.timing.constructIndex(searchApi1, folder1)
        //sleep 10 milliseconds to separate reading from saved index
        TimeUnit.MILLISECONDS.sleep(10)
        val indexingState2 = testCase.timing.constructIndex(searchApi2, folder2)
        //waiting finishing indexing for both states
        indexingState1.result.get()
        indexingState2.result.get()
        assertAll({ Assertions.assertTrue(searchApi1.hasIndexAtFolder(folder1), "searchApi1 has index for folder1") },
            { Assertions.assertTrue(searchApi2.hasIndexAtFolder(folder2), "searchApi2 has index for folder2") })
    }

    /**
     * Checking double indexing of 2 folders with negative scenarios
     * For each of these scenarios it should be thrown exception BusySearchException
     * when we try to calculate index for second folder.
     *
     * Scenarios have 4 options:
     *  - folder configuration:
     *      - same folder,
     *      - subfolder,
     *      - parent folder,
     *      - different folder
     *  - index presence - before indexing should be the index for each folder or no:
     *      - no index,
     *      - only fist folder,
     *      - only second folder,
     *   - number of instances of SearchApi
     *      - single, one for both folders
     *      - dual, each folder has own searchApi
     *   - timing of calculating index
     *      - concurrent - asynchronous
     *      - sequential - synchronous
     * */
    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeConcurrencyTestCaseProvider")
    fun negativeCombinationTest(testCase: ConcurrencyTestCase) {
        val folder1 = testCase.folderCombination.folder1
        val folder2 = testCase.folderCombination.folder2
        //preparing 1 or 2 instances of SearchApi
        val searchApi1 = searchApiGenerator()
        val searchApi2 = if (testCase.numberOfInstances == DUAL) searchApiGenerator() else searchApi1
        //preparing indices before main logic - synchronous indexing if needed for each folder
        if (testCase.indexPresence.hasFolder1Index) searchApi1.syncPerformIndex(folder1)
        if (testCase.indexPresence.hasFolder2Index) searchApi2.syncPerformIndex(folder2)
        //main indexing. Depends on timing synchronous or asynchronous
        //At first indexing first folder
        testCase.timing.constructIndex(searchApi1, folder1)
        //sleep 10 milliseconds to separate reading from saved index
        TimeUnit.MILLISECONDS.sleep(10)
        //When indexing second folder, throws exception BusySearchException
        Assertions.assertThrows(/* expectedType = */ BusySearchException::class.java,/* executable = */
            { testCase.timing.constructIndex(searchApi2, folder2) },/* message = */
            "Cannot invoke second time"
        )
    }

    /**
     * If we have calculated index in SearchApi1 and other SearchApi2 doesn't have prepared index
     * */
    @Test
    fun crossInstanceTest() {
        val folder = commonSetup.srcFolder
        val searchApi1 = searchApiGenerator()
        val searchApi2 = searchApiGenerator()
        searchApi1.syncPerformIndex(folder)
        Assertions.assertFalse(searchApi2.hasIndexAtFolder(folder))
    }

    /**
     * If we are calculating index for folder1, and try to calculate index for folder2, which is not a folder.
     * It throws exception NotDirSearchException.
     * */
    @Test
    fun noDirTest() {
        val folder1 = commonSetup.srcFolder
        val folder2 = commonSetup.commonPath.resolve("notFolder.txt")
        val searchApi = searchApiGenerator()
        searchApi.createIndexAtFolder(folder1)
        Assertions.assertThrows(
            /* expectedType = */ NotDirSearchException::class.java,
            /* executable = */ { searchApi.createIndexAtFolder(folder2) },
            /* message = */ "Folder2 is not really a folder"
        )
    }

    /**
     * Check that negativeConcurrencyTestCases and negativeConcurrencyTestCases
     * cover all combinations of ConcurrencyTestCase.
     * */
    @Test
    fun checkIsCompleteConcurrencyTestCasesTest() {
        val expectedCasesSet = buildSet {
            for (timing in Timing.entries) {
                for (indexPresence in IndexPresence.entries) {
                    for (numberOfInstances in NumberOfInstances.entries) {
                        for (folderCombination in FolderCombination.entries) {
                            add(ConcurrencyTestCase(timing, indexPresence, numberOfInstances, folderCombination))
                        }
                    }
                }
            }
        }
        val negativeSet = negativeConcurrencyTestCases.toSet()
        val positiveSet = positiveConcurrencyTestCases.toSet()
        val totalSet = negativeSet + positiveSet
        Assertions.assertAll(
            { compareSets(expectedCasesSet, totalSet, "all combinations", "positive + negative", "case") },
            {
                Assertions.assertTrue(
                    positiveSet.intersect(negativeSet).isEmpty(), "positive and negative set don't intersect"
                )
            },
            {
                Assertions.assertEquals(
                    positiveConcurrencyTestCases.size, positiveSet.size, "positive list doesn't have repeats"
                )
            },
            {
                Assertions.assertEquals(
                    negativeConcurrencyTestCases.size, negativeSet.size, "negative list doesn't have repeats"
                )
            },
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
         * Arguments with all negative ConcurrencyTestCases
         * */
        @JvmStatic
        fun negativeConcurrencyTestCaseProvider(): Stream<Arguments> {
            return negativeConcurrencyTestCases.stream().map { Arguments.of(it) }
        }

        /**
         * List of negative ConcurrencyTestCases
         * */
        val negativeConcurrencyTestCases = listOf(
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, SINGLE, DIFFERENT_FOLDER),

            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, SINGLE, DIFFERENT_FOLDER),
        )

        /**
         * List of positive ConcurrencyTestCases
         * */
        val positiveConcurrencyTestCases = listOf(
            //CONCURRENT - SINGLE
            //only second, but in fact it is both
            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, SINGLE, SAME_FOLDER),

            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, SINGLE, DIFFERENT_FOLDER),

            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, SINGLE, DIFFERENT_FOLDER),

            //CONCURRENT - DUAL
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, NO_INDEX, DUAL, DIFFERENT_FOLDER),

            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_SECOND, DUAL, DIFFERENT_FOLDER),

            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, ONLY_FIRST, DUAL, DIFFERENT_FOLDER),

            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(CONCURRENT, BOTH_INDICES, DUAL, DIFFERENT_FOLDER),

            //SEQUENCIAL
            //SEQUENCIAL - SINGLE
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, SINGLE, DIFFERENT_FOLDER),

            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, SINGLE, DIFFERENT_FOLDER),

            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, SINGLE, DIFFERENT_FOLDER),

            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, SINGLE, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, SINGLE, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, SINGLE, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, SINGLE, DIFFERENT_FOLDER),

            //SEQUENCIAL - DUAL
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, NO_INDEX, DUAL, DIFFERENT_FOLDER),

            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_SECOND, DUAL, DIFFERENT_FOLDER),

            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, ONLY_FIRST, DUAL, DIFFERENT_FOLDER),

            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, DUAL, SAME_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, DUAL, SUB_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, DUAL, PARENT_FOLDER),
            ConcurrencyTestCase(SEQUENCIAL, BOTH_INDICES, DUAL, DIFFERENT_FOLDER),
        )
    }
}