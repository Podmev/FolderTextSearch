package searchApi.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertAll
import java.nio.file.Path

/**
 * Compare trigramMaps with detailed exceptions if structures are not the same.
 * */
fun assertEqualsTrigramMap(expectedTrigramMap: Map<String, Set<Path>>, actualTrigramMap: Map<String, Set<Path>>) {
    val expectedTokens = expectedTrigramMap.keys
    val actualTokens = actualTrigramMap.keys
    val commonTokens = expectedTokens.intersect(actualTokens)

    val sizeChecks = listOf { ->
        Assertions.assertEquals(
            expectedTokens.size,
            actualTokens.size,
            "Compare number of found tokens and expected"
        )
    }
    val notFoundChecks = buildList<() -> Unit> {
        for (notFoundExpectedToken in expectedTokens.minus(actualTokens)) {
            add { -> Assertions.fail("Not found some expected token in actual: $notFoundExpectedToken") }
        }
    }
    val extraFoundChecks = buildList<() -> Unit> {
        for (extraTokenInActual in actualTokens.minus(expectedTokens)) {
            add { -> Assertions.fail("Found extra token in actual: $extraTokenInActual") }
        }
    }
    val commonMismatchChecks = commonTokens.flatMap {
        getAssertsForPathSets(
            token = it,
            expectedPathSet = expectedTrigramMap[it]!!, //we know it should exist, because token is common
            actualPathSet = actualTrigramMap[it]!! //we know it should exist, because token is common
        )
    }
    val totalChecks = sizeChecks + notFoundChecks + extraFoundChecks + commonMismatchChecks
    assertAll("compare search results", totalChecks)
}

/**
 * Compare sets of paths for token with detailed exceptions if structures are not the same.
 * */
fun getAssertsForPathSets(token: String, expectedPathSet: Set<Path>, actualPathSet: Set<Path>): List<() -> Unit> {
    val sizeChecks = listOf { ->
        Assertions.assertEquals(
            expectedPathSet.size,
            actualPathSet.size,
            "Compare for token $token number of found paths and expected"
        )
    }
    val notFoundChecks = buildList<() -> Unit> {
        for (notFoundExpectedPath in expectedPathSet.minus(actualPathSet)) {
            add { -> Assertions.fail("Not found for token: \"${token}\" expected path in actual: $notFoundExpectedPath") }
        }
    }
    val extraFoundChecks = buildList<() -> Unit> {
        for (extraPathInActual in actualPathSet.minus(expectedPathSet)) {
            add { -> Assertions.fail("Found for token: \"${token}\" extra path in actual: $extraPathInActual") }
        }
    }

    return sizeChecks + notFoundChecks + extraFoundChecks
}