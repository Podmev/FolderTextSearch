package searchApi.trigram

import api.ProgressableStatus
import api.ProgressableStatus.*
import impl.trigram.trigramChangeStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Testing changing statuses for TrigramSearchApi
 * */
class TrigramChangeStatusTest {
    /**
     * Parametrized test with all combinations
     * */
    @ParameterizedTest(name = "previousStatus:{0}, changingStatus:{1}, resultStatus:{2}")
    @MethodSource("statusChangesProvider")
    fun validChangeStatusTest(
        previousStatus: ProgressableStatus,
        changingStatus: ProgressableStatus,
        resultStatus: ProgressableStatus
    ) {
        Assertions.assertEquals(
            /* expected = */ resultStatus,
            /* actual = */  trigramChangeStatus(previousStatus, changingStatus)
        )
    }

    companion object {
        /**
         * List of all combinations of changing statuses
         * previousStatus, changingStatus, resultStatus
         * */
        @JvmStatic
        fun statusChangesProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(NOT_STARTED, NOT_STARTED, NOT_STARTED),
                Arguments.of(IN_PROGRESS, NOT_STARTED, IN_PROGRESS),
                Arguments.of(CANCELLING, NOT_STARTED, CANCELLING),
                Arguments.of(CANCELLED, NOT_STARTED, CANCELLED),
                Arguments.of(FINISHED, NOT_STARTED, FINISHED),
                Arguments.of(FAILED, NOT_STARTED, FAILED),

                Arguments.of(NOT_STARTED, IN_PROGRESS, IN_PROGRESS),
                Arguments.of(IN_PROGRESS, IN_PROGRESS, IN_PROGRESS),
                Arguments.of(CANCELLING, IN_PROGRESS, CANCELLING),
                Arguments.of(CANCELLED, IN_PROGRESS, CANCELLED),
                Arguments.of(FINISHED, IN_PROGRESS, FINISHED),
                Arguments.of(FAILED, IN_PROGRESS, FAILED),

                Arguments.of(NOT_STARTED, CANCELLING, CANCELLING),
                Arguments.of(IN_PROGRESS, CANCELLING, CANCELLING),
                Arguments.of(CANCELLING, CANCELLING, CANCELLING),
                Arguments.of(CANCELLED, CANCELLING, CANCELLED),
                Arguments.of(FINISHED, CANCELLING, FINISHED),
                Arguments.of(FAILED, CANCELLING, FAILED),

                Arguments.of(NOT_STARTED, CANCELLED, CANCELLED),
                Arguments.of(IN_PROGRESS, CANCELLED, CANCELLED),
                Arguments.of(CANCELLING, CANCELLED, CANCELLED),
                Arguments.of(CANCELLED, CANCELLED, CANCELLED),
                Arguments.of(FINISHED, CANCELLED, FINISHED),
                Arguments.of(FAILED, CANCELLED, FAILED),

                Arguments.of(NOT_STARTED, FINISHED, FINISHED),
                Arguments.of(IN_PROGRESS, FINISHED, FINISHED),
                Arguments.of(CANCELLING, FINISHED, CANCELLING),
                Arguments.of(CANCELLED, FINISHED, CANCELLED),
                Arguments.of(FINISHED, FINISHED, FINISHED),
                Arguments.of(FAILED, FINISHED, FAILED),

                Arguments.of(NOT_STARTED, FAILED, FAILED),
                Arguments.of(IN_PROGRESS, FAILED, FAILED),
                Arguments.of(CANCELLING, FAILED, FAILED),
                Arguments.of(CANCELLED, FAILED, CANCELLED),
                Arguments.of(FINISHED, FAILED, FINISHED),
                Arguments.of(FAILED, FAILED, FAILED),
            )
        }
    }
}