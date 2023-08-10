package searchApi.common

import org.junit.jupiter.api.Assertions

/**
 * Creates list of detailed checks for comparing sets.
 * So if 2 sets are different it would show all separate diferences.
 */
fun <T> compareSets(
    expectedSet: Set<T>,
    actualSet: Set<T>,
    expectedSetName: String,
    actualSetName: String,
    itemName: String
): List<() -> Unit> {
    val sizeChecks = listOf { ->
        Assertions.assertEquals(
            expectedSet.size,
            actualSet.size,
            "Compare number of found in $expectedSetName and $actualSetName}"
        )
    }
    val notFoundChecks = buildList<() -> Unit> {
        for (notFoundExpectedItem in expectedSet.minus(actualSet)) {
            add { -> Assertions.fail("Not found some expected $itemName in actual $actualSetName: $notFoundExpectedItem") }
        }
    }
    val extraFoundChecks = buildList<() -> Unit> {
        for (extraItemInActual in actualSet.minus(expectedSet)) {
            add { -> Assertions.fail("Found extra $itemName in actual $actualSetName: $extraItemInActual") }
        }
    }
    return sizeChecks + notFoundChecks + extraFoundChecks
}