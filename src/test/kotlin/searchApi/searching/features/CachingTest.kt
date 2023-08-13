package searchApi.searching.features

import api.SearchApi
import api.tools.searchapi.syncPerformIndex
import api.tools.searchapi.syncPerformSearch
import impl.trigram.TrigramSearchApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import searchApi.common.commonSetup
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Checking how cache works on search requests.
 * Tests show that there is no cache.
 * */
class CachingTest {
    /**
     * Generator of SearchApi, so every time we use it, it is with fresh state.
     * */
    private val searchApiGenerator: () -> SearchApi = { TrigramSearchApi() }

    /**
     * 10 same searches on same folder after inicial warmup search (witch is a bit longer).
     * Counting standard deviation of total time of all values.
     * It shouldn't be more than 50% from average value.
     * So it means that it is no cache. Every time search calculates independently by index.
     * */
    @Test
    fun repeatSearchWorksSameTimeTest() {
        val folder = commonSetup.srcFolder
        val token = "index"
        val searchApi = searchApiGenerator()
        searchApi.syncPerformIndex(folder)

        val stateForWarmUpJVM = searchApi.syncPerformSearch(folder, token)
        val firstTimeTokenMatches = stateForWarmUpJVM.result.get()
        val warmUpTotalTime = stateForWarmUpJVM.totalTime

        val states = (0 until 10).map { searchApi.syncPerformSearch(folder, token) }
        val totalTimes = states.map { it.totalTime }
        val averageTotalTime = totalTimes.sum().toDouble() / totalTimes.size
        val standardDeviationTotalTime = sqrt(
            (totalTimes.sumOf { (it.toDouble() - averageTotalTime).pow(2.0) }) / totalTimes.size
        )
        val standardDeviationRatio = standardDeviationTotalTime / averageTotalTime
        val warmUpMaxChecks = totalTimes.withIndex().map { (index, totalTime) ->
            { assertTrue(totalTime < warmUpTotalTime, "#$index totalTime < warmUpTotalTime") }
        }
        val warmUpMinChecks = totalTimes.map { it.toDouble() }.withIndex().map { (index, totalTime) ->
            { assertTrue(totalTime > 0.2 * warmUpTotalTime, "#$index totalTime > 0.2*warmUpTotalTime") }
        }
        val equalsChecks = states.map { it.result.get().size }.withIndex().map { (index, resultSize) ->
            { assertEquals(firstTimeTokenMatches.size, resultSize, "#$index result is the same as first time") }
        }
        val checks = warmUpMinChecks + warmUpMaxChecks + equalsChecks
        assertAll({ assertEquals(10, totalTimes.size, "There are 10 tokens") }, {
            assertTrue(
                standardDeviationRatio < 0.5,
                "Standard deviation / average total time < 0.5 ($standardDeviationRatio)"
            )
        }, *checks.toTypedArray()
        )
    }
}