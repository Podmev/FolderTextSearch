package searchApi.trigram

import api.ProgressableStatus
import api.ProgressableStatus.*
import impl.trigram.trigramAggregateIndexAndSearchStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Testing aggregating index and search statuses for TrigramSearchApi
 * */
class TrigramAggregateIndexAndSearchStatusTest {
    /**
     * Parametrized test with all combinations
     * */
    @ParameterizedTest(name = "indexStatus:{0}, searchStatus:{1}, aggregatedStatus:{2}")
    @MethodSource("indexAndSearchStatusProvider")
    fun aggregateIndexAndSearchStatusTest(
        indexStatus: ProgressableStatus,
        searchStatus: ProgressableStatus,
        aggregatedStatus: ProgressableStatus
    ) {
        Assertions.assertEquals(
            /* expected = */ aggregatedStatus,
            /* actual = */  trigramAggregateIndexAndSearchStatus(indexStatus, searchStatus)
        )
    }

    companion object {
        /**
         * List of all combinations of aggregating index and search statuses
         * indexStatus, searchStatus, aggregatedStatus
         * */
        @JvmStatic
        fun indexAndSearchStatusProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(NOT_STARTED, NOT_STARTED, NOT_STARTED),
                Arguments.of(IN_PROGRESS, NOT_STARTED, IN_PROGRESS),
                Arguments.of(CANCELLING, NOT_STARTED, CANCELLING),
                Arguments.of(CANCELLED, NOT_STARTED, CANCELLED),
                Arguments.of(FINISHED, NOT_STARTED, IN_PROGRESS),
                Arguments.of(FAILED, NOT_STARTED, FAILED),

                Arguments.of(NOT_STARTED, IN_PROGRESS, FAILED),
                Arguments.of(IN_PROGRESS, IN_PROGRESS, FAILED),
                Arguments.of(CANCELLING, IN_PROGRESS, FAILED),
                Arguments.of(CANCELLED, IN_PROGRESS, FAILED),
                Arguments.of(FINISHED, IN_PROGRESS, IN_PROGRESS),
                Arguments.of(FAILED, IN_PROGRESS, FAILED),

                Arguments.of(NOT_STARTED, CANCELLING, CANCELLING),
                Arguments.of(IN_PROGRESS, CANCELLING, CANCELLING),
                Arguments.of(CANCELLING, CANCELLING, CANCELLING),
                Arguments.of(CANCELLED, CANCELLING, CANCELLED),
                Arguments.of(FINISHED, CANCELLING, CANCELLING),
                Arguments.of(FAILED, CANCELLING, FAILED),

                Arguments.of(NOT_STARTED, CANCELLED, CANCELLED),
                Arguments.of(IN_PROGRESS, CANCELLED, CANCELLED),
                Arguments.of(CANCELLING, CANCELLED, CANCELLED),
                Arguments.of(CANCELLED, CANCELLED, CANCELLED),
                Arguments.of(FINISHED, CANCELLED, CANCELLED),
                Arguments.of(FAILED, CANCELLED, FAILED),

                Arguments.of(NOT_STARTED, FINISHED, FAILED),
                Arguments.of(IN_PROGRESS, FINISHED, FAILED),
                Arguments.of(CANCELLING, FINISHED, FAILED),
                Arguments.of(CANCELLED, FINISHED, CANCELLED),
                Arguments.of(FINISHED, FINISHED, FINISHED),
                Arguments.of(FAILED, FINISHED, FAILED),

                Arguments.of(NOT_STARTED, FAILED, FAILED),
                Arguments.of(IN_PROGRESS, FAILED, FAILED),
                Arguments.of(CANCELLING, FAILED, FAILED),
                Arguments.of(CANCELLED, FAILED, FAILED),
                Arguments.of(FINISHED, FAILED, FAILED),
                Arguments.of(FAILED, FAILED, FAILED),
            )
        }
    }
}